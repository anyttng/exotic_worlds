package com.exoticworlds.compat.terrablender;

import static com.exoticworlds.engine.noise.ClimateScaleCompression.NO_COMPRESSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import com.exoticworlds.compat.terrablender.RegionLayerFold.LayerAxis;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;

import net.minecraft.core.Direction;
import net.minecraft.core.QuartPos;

import terrablender.worldgen.noise.Area;
import terrablender.worldgen.noise.AreaContext;
import terrablender.worldgen.noise.ZoomLayer;

class RegionLayerFoldTest {
    private static final long SEED = 710L;
    private static final int REGIONS = 7;
    private static final int MAX_CACHE = 1024;
    private static final int CONTEXT_CACHE = 25;

    private static final long INITIAL_MODIFIER = 1L;
    private static final long FUZZY_MODIFIER = 2000L;
    private static final long FIXED_ZOOM_MODIFIER = 2001L;
    private static final long SIZE_ZOOM_MODIFIER = 1001L;
    private static final int FIXED_ZOOMS = 3;

    private static final int DEFAULT_REGION_SIZE = 3;
    private static final int[] REGION_SIZES = {2, 3, 4, 6};
    private static final int[] CHUNK_WIDTHS = {32, 48, 64, 128, 750, 2500};

    private static final int SAMPLE_LINES = 5;
    private static final int SAMPLES_PER_LINE = 400;

    private static final int SWEEP_MIN_CHUNKS = 16;
    private static final int SWEEP_MAX_CHUNKS = 3000;
    private static final double MAX_STRETCH = 2.0;

    private static final double[] FACTORS = {NO_COMPRESSION, 2.5, 4.0};
    private static final double[] COMPRESSED_FACTORS = {2.0, 2.5, 4.0};
    private static final int SIZE_Z_STRIDE = 13;
    private static final int SEAM_STRIP_QUARTS = 64;

    private static WorldFold torus(int chunkWidth) {
        return WorldFolds.of(FlatShape.torus(WorldLoopBounds.ofWidth(chunkWidth)));
    }

    private static WorldFold latticeTorus(int chunkWidth, int skewChunks) {
        return WorldFolds.of(FlatShape.latticeTorus(WorldLoopBounds.ofWidth(chunkWidth), skewChunks));
    }

    private static int topDepth(int regionSize) {
        return 1 + FIXED_ZOOMS + regionSize;
    }

    private static int foldX(@Nullable RegionLayerFold fold, int depth, int x, int z) {
        return fold == null ? x : fold.foldX(depth, x, z);
    }

    private static int foldZ(@Nullable RegionLayerFold fold, int depth, int z) {
        return fold == null ? z : fold.foldZ(depth, z);
    }

    private static Area regionMap(int regionSize, @Nullable RegionLayerFold fold) {
        AreaContext initialContext = new AreaContext(CONTEXT_CACHE, SEED, INITIAL_MODIFIER);
        Area area = new Area((x, z) -> {
            int foldedX = foldX(fold, 0, x, z);
            int foldedZ = foldZ(fold, 0, z);
            initialContext.initRandom(foldedX, foldedZ);
            return initialContext.nextRandom(REGIONS);
        }, CONTEXT_CACHE);

        area = zoom(area, ZoomLayer.FUZZY, FUZZY_MODIFIER, 1, fold);
        for (int zoom = 0; zoom < FIXED_ZOOMS; zoom++) {
            area = zoom(area, ZoomLayer.NORMAL, FIXED_ZOOM_MODIFIER + zoom, 2 + zoom, fold);
        }

        for (int zoom = 0; zoom < regionSize; zoom++) {
            area = zoom(area, ZoomLayer.NORMAL, SIZE_ZOOM_MODIFIER + zoom, 2 + FIXED_ZOOMS + zoom, fold);
        }

        return area;
    }

    private static Area zoom(Area parent, ZoomLayer layer, long modifier, int depth, @Nullable RegionLayerFold fold) {
        AreaContext context = new AreaContext(CONTEXT_CACHE, SEED, modifier);
        return new Area((x, z) -> {
            int foldedX = foldX(fold, depth, x, z);
            int foldedZ = foldZ(fold, depth, z);
            context.initRandom(foldedX, foldedZ);
            return layer.apply(context, parent, foldedX, foldedZ);
        }, MAX_CACHE);
    }

    @Test
    void foldedMapReadsTheSameRegionOneLapAwayOnBothAxes() {
        for (double factor : FACTORS) {
            for (int regionSize : REGION_SIZES) {
                for (int chunkWidth : CHUNK_WIDTHS) {
                    assertPeriodic(RegionLayerFold.of(torus(chunkWidth), topDepth(regionSize), factor), regionSize,
                            chunkWidth + " chunks, region size " + regionSize + ", factor " + factor);
                }
            }
        }
    }

    @Test
    void latticeMapReadsTheSameRegionAtEveryLatticeCopy() {
        for (double factor : FACTORS) {
            for (int chunkWidth : CHUNK_WIDTHS) {
                for (int skewChunks : skewsOf(chunkWidth)) {
                    String world = chunkWidth + " chunks, skew " + skewChunks + " chunks, factor " + factor;
                    RegionLayerFold fold = RegionLayerFold.of(latticeTorus(chunkWidth, skewChunks),
                            topDepth(DEFAULT_REGION_SIZE), factor);
                    assertLatticePeriodic(regionMap(DEFAULT_REGION_SIZE, fold), fold.x(),
                            QuartPos.fromSection(skewChunks), world);
                }
            }
        }
    }

    private static int[] skewsOf(int chunkWidth) {
        return new int[] {1, chunkWidth / 3, -chunkWidth / 4, chunkWidth / 2};
    }

    private static void assertLatticePeriodic(Area map, LayerAxis axis, int skewQuarts, String world) {
        int lap = axis.lap();
        int stride = Math.max(1, lap / SAMPLES_PER_LINE);
        for (int line = 0; line < SAMPLE_LINES; line++) {
            int cross = axis.min() + line * lap / SAMPLE_LINES;
            for (int along = axis.min() - lap; along < axis.min() + lap; along += stride) {
                assertEquals(map.get(along, cross), map.get(along + lap, cross),
                        "X quart " + along + " in " + world);
                assertEquals(map.get(cross, along), map.get(cross + skewQuarts, along + lap),
                        "Z quart " + along + " in " + world);
            }
        }

        for (int along = axis.min() - SEAM_STRIP_QUARTS; along < axis.min() + SEAM_STRIP_QUARTS; along++) {
            assertEquals(map.get(0, along), map.get(skewQuarts, along + lap),
                    "Z seam quart " + along + " in " + world);
        }
    }

    private static void assertPeriodic(RegionLayerFold fold, int regionSize, String world) {
        Area map = regionMap(regionSize, fold);
        LayerAxis axis = fold.x();
        int stride = Math.max(1, axis.lap() / SAMPLES_PER_LINE);
        for (int line = 0; line < SAMPLE_LINES; line++) {
            int cross = axis.origin() + line * axis.lap() / SAMPLE_LINES;
            for (int along = axis.origin() - axis.lap(); along < axis.origin() + axis.lap(); along += stride) {
                assertEquals(map.get(along, cross), map.get(along + axis.lap(), cross),
                        "X quart " + along + " in " + world);
                assertEquals(map.get(cross, along), map.get(cross, along + axis.lap()),
                        "Z quart " + along + " in " + world);
            }
        }
    }

    @Test
    void seamStripReadsTheSameRegionOneLapAwayAtEveryQuart() {
        for (int chunkWidth : CHUNK_WIDTHS) {
            RegionLayerFold fold = RegionLayerFold.of(torus(chunkWidth), topDepth(DEFAULT_REGION_SIZE), NO_COMPRESSION);
            Area map = regionMap(DEFAULT_REGION_SIZE, fold);
            LayerAxis axis = fold.x();
            int seam = axis.origin() + axis.lap() - axis.strip();
            for (int along = seam - axis.cell(); along < seam + axis.strip() + axis.cell(); along++) {
                assertEquals(map.get(along, 0), map.get(along - axis.lap(), 0),
                        "quart " + along + " in " + chunkWidth + " chunks");
            }
        }
    }

    @Test
    void wholeCellsInsideTheWorldKeepTerraBlendersLayout() {
        for (int chunkWidth : CHUNK_WIDTHS) {
            RegionLayerFold fold = RegionLayerFold.of(torus(chunkWidth), topDepth(DEFAULT_REGION_SIZE), NO_COMPRESSION);
            Area folded = regionMap(DEFAULT_REGION_SIZE, fold);
            Area unfolded = regionMap(DEFAULT_REGION_SIZE, null);
            LayerAxis axis = fold.x();
            int untouchedEnd = axis.origin() + axis.interior() - 2 * axis.cell();
            int stride = Math.max(1, axis.lap() / SAMPLES_PER_LINE);

            for (int x = axis.origin(); x < untouchedEnd; x += stride) {
                for (int z = axis.origin(); z < untouchedEnd; z += axis.cell()) {
                    assertEquals(unfolded.get(x, z), folded.get(x, z),
                            "quart " + x + ", " + z + " in " + chunkWidth + " chunks");
                }
            }
        }
    }

    @Test
    void unboundedAxisIsNeverFolded() {
        WorldFold cylinder = WorldFolds.of(FlatShape.cylinder(WorldLoopBounds.ofWidth(Direction.Axis.X, 64)));
        RegionLayerFold fold = RegionLayerFold.of(cylinder, topDepth(DEFAULT_REGION_SIZE), NO_COMPRESSION);
        for (int depth = 0; depth <= topDepth(DEFAULT_REGION_SIZE); depth++) {
            for (int coord = -5000; coord < 5000; coord += 37) {
                assertEquals(coord, fold.foldZ(depth, coord));
            }
        }
    }

    @Test
    void virtualLapIsWholeCellsAndStretchesTheSeamStripAtMostTwofold() {
        for (double factor : FACTORS) {
            for (int regionSize : REGION_SIZES) {
                for (int chunkWidth = SWEEP_MIN_CHUNKS; chunkWidth <= SWEEP_MAX_CHUNKS; chunkWidth++) {
                    assertWholeCells(RegionLayerFold.of(torus(chunkWidth), topDepth(regionSize), factor).x(),
                            chunkWidth + " chunks, region size " + regionSize + ", factor " + factor);
                }
            }
        }
    }

    private static void assertWholeCells(LayerAxis axis, String world) {
        assertEquals(0, axis.virtualLap() % axis.cell(), world);
        assertEquals(0, axis.origin() % axis.cell(), world);
        if (axis.strip() == 0 || axis.interior() == 0) {
            return;
        }

        int stretchedStart = axis.stripCells() > 0 ? axis.interior() : axis.interior() - axis.cell();
        double ratio = (double) (axis.virtualLap() - stretchedStart) / (axis.scaledLap() - stretchedStart);
        assertTrue(ratio <= MAX_STRETCH && ratio >= 1 / MAX_STRETCH, world + ": stretch " + ratio);
    }

    @Test
    void compressedMapInsideTheWorldReadsTerraBlendersMapAtTheScaledQuart() {
        for (double factor : COMPRESSED_FACTORS) {
            for (int chunkWidth : CHUNK_WIDTHS) {
                RegionLayerFold fold = RegionLayerFold.of(torus(chunkWidth), topDepth(DEFAULT_REGION_SIZE), factor);
                Area folded = regionMap(DEFAULT_REGION_SIZE, fold);
                Area unfolded = regionMap(DEFAULT_REGION_SIZE, null);
                LayerAxis axis = fold.x();
                int untouchedEnd = axis.origin() + axis.interior() - 2 * axis.cell();
                int stride = Math.max(1, axis.lap() / SAMPLES_PER_LINE);
                String world = chunkWidth + " chunks, factor " + factor;
                for (int x = axis.min(); x < axis.min() + axis.lap(); x += stride) {
                    int scaledX = axis.min() + (int) Math.floor((x - axis.min()) * factor);
                    for (int z = axis.min(); z < axis.min() + axis.lap(); z += SIZE_Z_STRIDE) {
                        int scaledZ = axis.min() + (int) Math.floor((z - axis.min()) * factor);
                        if (inside(scaledX, axis.origin(), untouchedEnd)
                                && inside(scaledZ, axis.origin(), untouchedEnd)) {
                            assertEquals(unfolded.get(scaledX, scaledZ), folded.get(x, z),
                                    "quart " + x + ", " + z + " in " + world);
                        }
                    }
                }
            }
        }
    }

    private static boolean inside(int coord, int from, int to) {
        return coord >= from && coord < to;
    }

    @Test
    void unboundedAxisIsScaledAtTheTopLayerAlone() {
        double factor = 2.5;
        int topDepth = topDepth(DEFAULT_REGION_SIZE);
        WorldFold cylinder = WorldFolds.of(FlatShape.cylinder(WorldLoopBounds.ofWidth(Direction.Axis.X, 64)));
        RegionLayerFold fold = RegionLayerFold.of(cylinder, topDepth, factor);
        for (int coord = -5000; coord < 5000; coord += 37) {
            assertEquals((int) Math.floor(coord * factor), fold.foldZ(topDepth, coord));
            for (int depth = 0; depth < topDepth; depth++) {
                assertEquals(coord, fold.foldZ(depth, coord));
            }
        }
    }
}
