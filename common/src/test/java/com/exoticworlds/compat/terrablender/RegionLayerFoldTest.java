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

    private static WorldFold torus(int chunkWidth) {
        return WorldFolds.of(FlatShape.torus(WorldLoopBounds.ofWidth(chunkWidth)));
    }

    private static int topDepth(int regionSize) {
        return 1 + FIXED_ZOOMS + regionSize;
    }

    private interface LayerFold {
        int apply(Direction.Axis axis, int depth, int coord);
    }

    private static Area regionMap(int regionSize, @Nullable RegionLayerFold fold) {
        LayerFold layerFold = fold == null ? (axis, depth, coord) -> coord : fold::apply;
        AreaContext initialContext = new AreaContext(CONTEXT_CACHE, SEED, INITIAL_MODIFIER);
        Area area = new Area((x, z) -> {
            int foldedX = layerFold.apply(Direction.Axis.X, 0, x);
            int foldedZ = layerFold.apply(Direction.Axis.Z, 0, z);
            initialContext.initRandom(foldedX, foldedZ);
            return initialContext.nextRandom(REGIONS);
        }, CONTEXT_CACHE);

        area = zoom(area, ZoomLayer.FUZZY, FUZZY_MODIFIER, 1, layerFold);
        for (int zoom = 0; zoom < FIXED_ZOOMS; zoom++) {
            area = zoom(area, ZoomLayer.NORMAL, FIXED_ZOOM_MODIFIER + zoom, 2 + zoom, layerFold);
        }

        for (int zoom = 0; zoom < regionSize; zoom++) {
            area = zoom(area, ZoomLayer.NORMAL, SIZE_ZOOM_MODIFIER + zoom, 2 + FIXED_ZOOMS + zoom, layerFold);
        }

        return area;
    }

    private static Area zoom(Area parent, ZoomLayer layer, long modifier, int depth, LayerFold layerFold) {
        AreaContext context = new AreaContext(CONTEXT_CACHE, SEED, modifier);
        return new Area((x, z) -> {
            int foldedX = layerFold.apply(Direction.Axis.X, depth, x);
            int foldedZ = layerFold.apply(Direction.Axis.Z, depth, z);
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
                assertEquals(coord, fold.apply(Direction.Axis.Z, depth, coord));
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
            assertEquals((int) Math.floor(coord * factor), fold.apply(Direction.Axis.Z, topDepth, coord));
            for (int depth = 0; depth < topDepth; depth++) {
                assertEquals(coord, fold.apply(Direction.Axis.Z, depth, coord));
            }
        }
    }
}
