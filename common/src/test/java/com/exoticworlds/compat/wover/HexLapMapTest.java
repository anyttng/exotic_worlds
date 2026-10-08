package com.exoticworlds.compat.wover;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.betterx.wover.generator.api.biomesource.WoverBiomePicker;
import org.betterx.wover.generator.impl.map.hex.HexBiomeChunk;
import org.betterx.wover.generator.impl.map.hex.HexBiomeMap;
import org.betterx.wover.util.RandomizedWeightedList;
import org.junit.jupiter.api.Test;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WrapDomain;
import com.exoticworlds.engine.noise.ClimateScaleCompression;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.WorldgenRandom;

class HexLapMapTest {
    private static final int BIOME_SIZE = 256;

    private static final float DEFAULT_SCALE = BIOME_SIZE / 8.0F;

    private static final double FACTOR = 4.0;

    private static final int SEED = 0x900;

    private static final int PALETTE = 16;

    private static final int MIN_STRIDE_BLOCKS = 3;

    private static final int SAMPLES_PER_LAP = 256;

    private static final int SEAM_STRIP_BLOCKS = 96;

    private static final int WIDE_LAP_CHUNKS = 256;

    private static final int WIDE_LAP_BLOCKS = 4096;

    private static final int QUART = 4;

    private static final int REGION_LINES = 8;

    private static final int CHUNK_SAMPLES = 64;

    private static final double[] LINE_OFFSETS = {2.0, 6.0, 10.0, 14.0, 18.0, 22.0, 26.0, 30.0};

    private static final int[] TORUS_CHUNK_SIDES = {27, 28, 32};

    private static final double HALF_SAME = 0.5;

    private static final double REGION_TOLERANCE = 0.05;

    private static final double SAME_TOLERANCE = 0.005;

    private static final String PALETTE_NAMESPACE = "cject";

    private static final String PALETTE_PATH = "palette_";

    private static final String SUB_BIOMES_FIELD = "subbiomes";

    private static final double SUB_BIOME_WEIGHT = 1.0;

    private static final int SUB_BIOMES_EACH = 2;

    private static final double WORLD_WEAVER_ROTATION = 0.4;

    private static final LapPicker<Integer> PICKER = new LapPicker<>(
            random -> random.nextInt(PALETTE), (biome, random) -> biome);

    private interface Cells {
        Object at(double x, double z);
    }

    private record Regions(double halfDistance, double sameAtOneQuart) {
        static Regions of(Cells cells) {
            return of(cells, 0.0);
        }

        static Regions of(Cells rawCells, double angle) {
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            Cells cells = (x, z) -> rawCells.at(x * cos - z * sin, x * sin + z * cos);
            int lapQuarts = WIDE_LAP_BLOCKS / QUART;
            int maxDistance = lapQuarts / 2;
            long[] same = new long[maxDistance + 1];
            long[] compared = new long[maxDistance + 1];
            for (Direction.Axis axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
                for (double offset : LINE_OFFSETS) {
                    for (int line = 0; line < REGION_LINES; line++) {
                        double cross = -WIDE_LAP_BLOCKS / 2.0 + (double) line * WIDE_LAP_BLOCKS / REGION_LINES
                                + offset;
                        Object[] picks = new Object[lapQuarts + maxDistance];
                        for (int quart = 0; quart < picks.length; quart++) {
                            double along = -WIDE_LAP_BLOCKS / 2.0 + (double) quart * QUART;
                            picks[quart] = axis == Direction.Axis.X ? cells.at(along, cross) : cells.at(cross, along);
                        }

                        for (int start = 0; start < lapQuarts; start++) {
                            for (int distance = 1; distance <= maxDistance; distance++) {
                                compared[distance]++;
                                if (picks[start].equals(picks[start + distance])) {
                                    same[distance]++;
                                }
                            }
                        }
                    }
                }
            }

            return new Regions(halfDistance(same, compared), (double) same[1] / compared[1]);
        }

        private static double halfDistance(long[] same, long[] compared) {
            double before = 1.0;
            for (int distance = 1; distance < same.length; distance++) {
                double share = (double) same[distance] / compared[distance];
                if (share <= HALF_SAME) {
                    return distance - 1 + (before - HALF_SAME) / (before - share);
                }

                before = share;
            }

            return Double.NaN;
        }

        @Override
        public String toString() {
            return String.format(Locale.ROOT, "half at %.2f quarts, same at 1 quart %.3f", this.halfDistance,
                    this.sameAtOneQuart);
        }
    }

    private static final class Palette extends WoverBiomePicker {
        private final PickableBiome[] biomes = new PickableBiome[PALETTE];

        Palette() {
            this(0);
        }

        Palette(int subBiomesEach) {
            super((HolderGetter<Biome>) null, key(0));
            for (int index = 0; index < PALETTE; index++) {
                this.biomes[index] = new WoverBiomePicker((HolderGetter<Biome>) null, key(index)).fallbackBiome;
                for (int sub = 0; sub < subBiomesEach; sub++) {
                    addSubBiome(this.biomes[index], new WoverBiomePicker((HolderGetter<Biome>) null,
                            key(PALETTE + index * subBiomesEach + sub)).fallbackBiome);
                }
            }
        }

        @SuppressWarnings("unchecked")
        private static void addSubBiome(PickableBiome parent, PickableBiome sub) {
            try {
                Field subBiomes = PickableBiome.class.getDeclaredField(SUB_BIOMES_FIELD);
                subBiomes.setAccessible(true);
                ((RandomizedWeightedList<PickableBiome>) subBiomes.get(parent)).add(sub, SUB_BIOME_WEIGHT);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public PickableBiome getBiome(WorldgenRandom random) {
            return this.biomes[random.nextInt(PALETTE)];
        }

        private static ResourceKey<Biome> key(int index) {
            return ResourceKey.create(Registries.BIOME,
                    Identifier.fromNamespaceAndPath(PALETTE_NAMESPACE, PALETTE_PATH + index));
        }
    }

    private static HexLapMap<Integer> map(WorldFold fold, float scale) {
        return map(fold, scale, ClimateScaleCompression.NO_COMPRESSION);
    }

    private static HexLapMap<Integer> map(WorldFold fold, float scale, double factor) {
        return new HexLapMap<>(fold, scale, factor, SEED, PICKER);
    }

    @Test
    void aTorusNarrowerThanOneBlockReadsTheSameBiomeOneLapAwayOnBothAxes() {
        assertPeriodic(WorldFolds.of(FlatShape.torus(new WorldLoopBounds(-8, 8, -8, 8))), DEFAULT_SCALE);
    }

    @Test
    void aTorusOfSeveralBlocksReadsTheSameBiomeOneLapAwayOnBothAxes() {
        assertPeriodic(WorldFolds.of(FlatShape.torus(new WorldLoopBounds(-128, 128, -128, 128))), DEFAULT_SCALE);
    }

    @Test
    void aTorusWhoseLapSplitsIntoUnevenBlocksStillRepeats() {
        assertPeriodic(WorldFolds.of(FlatShape.torus(new WorldLoopBounds(-75, 75, -41, 41))), DEFAULT_SCALE);
    }

    @Test
    void aCylinderRepeatsOnItsLoopedAxisAndKeepsTheOtherOpen() {
        WorldFold cylinder = WorldFolds.of(FlatShape.cylinder(WorldLoopBounds.ofWidth(Direction.Axis.X, 256)));
        HexLapMap<Integer> map = map(cylinder, DEFAULT_SCALE);
        int lap = cylinder.blockDomain(Direction.Axis.X).domainLength;
        for (int z = -4096; z < 4096; z += 37) {
            for (int x = -2 * lap; x < 2 * lap; x += 101) {
                assertEquals(map.biomeAt(x, z), map.biomeAt(x + lap, z), "x " + x + " z " + z);
            }
        }

        assertFalse(map.axis(Direction.Axis.Z).loops(), "the open axis was given a lap");
    }

    @Test
    void aLatticeKeepsWorldWeaversCellOrShrinksItOnlyToFitAWholeCount() {
        int[] chunkWidths = {1, 3, 16, 41, 75, 128, 256, 1875};
        float[] scales = {DEFAULT_SCALE, 4.0F, 128.0F};
        for (int width : chunkWidths) {
            WorldFold fold = WorldFolds.of(FlatShape.torus(WorldLoopBounds.ofWidth(width)));
            for (float scale : scales) {
                HexLapMap<Integer> map = map(fold, scale);
                assertCell(map.axis(Direction.Axis.X), scale / HexLapMap.RAD_INNER, 1);
                assertCell(map.axis(Direction.Axis.Z), scale, 2);
            }
        }
    }

    @Test
    void theCompactBiomesFactorShrinksTheCellOnBothAxes() {
        double[] factors = {2.0, 4.0, 1.56};
        WorldFold torus = WorldFolds.of(FlatShape.torus(WorldLoopBounds.ofWidth(256)));
        for (double factor : factors) {
            HexLapMap<Integer> map = map(torus, DEFAULT_SCALE, factor);
            assertCell(map.axis(Direction.Axis.X), DEFAULT_SCALE / factor / HexLapMap.RAD_INNER, 1);
            assertCell(map.axis(Direction.Axis.Z), DEFAULT_SCALE / factor, 2);
        }

        WorldFold cylinder = WorldFolds.of(FlatShape.cylinder(WorldLoopBounds.ofWidth(Direction.Axis.X, 256)));
        HexLapMap<Integer> compact = map(cylinder, DEFAULT_SCALE, FACTOR);
        assertEquals(DEFAULT_SCALE / FACTOR, compact.axis(Direction.Axis.Z).cellBlocks(), 1.0E-9,
                "the cylinder's open axis kept WorldWeaver's cell");
        assertCell(compact.axis(Direction.Axis.X), DEFAULT_SCALE / FACTOR / HexLapMap.RAD_INNER, 1);
        assertPeriodic(cylinder, DEFAULT_SCALE, FACTOR);
    }

    @Test
    void aLapHeldInOneBlockSeedsTwoLinesAlongEachLoopedAxis() {
        assertEquals(2, HexLapChunk.seedLines(7, true));
        assertEquals(2, HexLapChunk.seedLines(2, true));
        assertEquals(1, HexLapChunk.seedLines(1, true));
        assertEquals(4, HexLapChunk.seedLines(32, true));
        assertEquals(4, HexLapChunk.seedLines(32, false));
    }

    @Test
    void theSameFoldAndSeedLayTheSameMap() {
        WorldFold fold = WorldFolds.of(FlatShape.torus(new WorldLoopBounds(-40, 40, -40, 40)));
        HexLapMap<Integer> first = map(fold, DEFAULT_SCALE);
        HexLapMap<Integer> second = map(fold, DEFAULT_SCALE);
        Set<Integer> seen = new HashSet<>();
        for (int x = -640; x < 640; x += 11) {
            for (int z = -640; z < 640; z += 13) {
                int biome = first.biomeAt(x, z);
                seen.add(biome);
                assertEquals(biome, second.biomeAt(x, z), "x " + x + " z " + z);
            }
        }

        assertTrue(seen.size() > 1, "the map laid one biome over the whole lap: " + seen);
    }

    @Test
    void theLapMapKeepsWorldWeaversRegionSizeOnALapWiderThanItsCell() {
        Palette palette = new Palette();
        HexBiomeMap own = new HexBiomeMap(SEED, BIOME_SIZE, palette);
        LapPicker<WoverBiomePicker.PickableBiome> picker = new LapPicker<>(palette::getBiome,
                (biome, random) -> biome);
        WorldFold torus = WorldFolds.of(FlatShape.torus(WorldLoopBounds.ofWidth(WIDE_LAP_CHUNKS)));
        HexLapMap<WoverBiomePicker.PickableBiome> onTorus = new HexLapMap<>(torus, DEFAULT_SCALE,
                ClimateScaleCompression.NO_COMPRESSION, SEED, picker);
        HexLapMap<WoverBiomePicker.PickableBiome> unbounded = new HexLapMap<>(WorldFolds.NOOP, DEFAULT_SCALE,
                ClimateScaleCompression.NO_COMPRESSION, SEED, picker);
        Regions ownMap = Regions.of((x, z) -> own.getBiome(x, 0.0, z));
        Regions lapOnTorus = Regions.of(onTorus::biomeAt);
        String readings = "WorldWeaver's own map: " + ownMap + "; the lap map on a " + WIDE_LAP_BLOCKS
                + "-block torus: " + lapOnTorus + "; unbounded: " + Regions.of(unbounded::biomeAt);
        assertEquals(1.0, lapOnTorus.halfDistance() / ownMap.halfDistance(), REGION_TOLERANCE, readings);
        assertEquals(ownMap.sameAtOneQuart(), lapOnTorus.sameAtOneQuart(), SAME_TOLERANCE, readings);
    }

    @Test
    void theLapMapKeepsWorldWeaversRegionSizeWithSubBiomes() {
        Palette palette = new Palette(SUB_BIOMES_EACH);
        HexBiomeMap own = new HexBiomeMap(SEED, BIOME_SIZE, palette);
        LapPicker<WoverBiomePicker.PickableBiome> picker = new LapPicker<>(palette::getBiome,
                WoverBiomePicker.PickableBiome::getSubBiome);
        WorldFold torus = WorldFolds.of(FlatShape.torus(WorldLoopBounds.ofWidth(WIDE_LAP_CHUNKS)));
        HexLapMap<WoverBiomePicker.PickableBiome> onTorus = new HexLapMap<>(torus, DEFAULT_SCALE,
                ClimateScaleCompression.NO_COMPRESSION, SEED, picker);
        Regions ownMap = Regions.of((x, z) -> own.getBiome(x, 0.0, z));
        Regions lapOnTorus = Regions.of(onTorus::biomeAt, -WORLD_WEAVER_ROTATION);
        String readings = "WorldWeaver's own map: " + ownMap + "; the lap map on a " + WIDE_LAP_BLOCKS
                + "-block torus, read at WorldWeaver's rotation: " + lapOnTorus;
        assertEquals(1.0, lapOnTorus.halfDistance() / ownMap.halfDistance(), REGION_TOLERANCE, readings);
        assertEquals(ownMap.sameAtOneQuart(), lapOnTorus.sameAtOneQuart(), SAME_TOLERANCE, readings);
    }

    @Test
    void theLapChunkLaysCellBordersAsOftenAsWorldWeaversChunk() {
        Palette palette = new Palette();
        assertCellBordersMatch(palette, new LapPicker<>(palette::getBiome, (biome, random) -> biome));
    }

    @Test
    void theLapChunkLaysCellBordersAsOftenAsWorldWeaversChunkWithSubBiomes() {
        Palette palette = new Palette(SUB_BIOMES_EACH);
        assertCellBordersMatch(palette,
                new LapPicker<>(palette::getBiome, WoverBiomePicker.PickableBiome::getSubBiome));
    }

    private static void assertCellBordersMatch(Palette palette, LapPicker<WoverBiomePicker.PickableBiome> picker) {
        long ownBorders = 0;
        long ownPairs = 0;
        for (int chunk = 0; chunk < CHUNK_SAMPLES; chunk++) {
            HexBiomeChunk own = new HexBiomeChunk(new WorldgenRandom(RandomSource.create(SEED + chunk)), palette);
            for (int x = 0; x < HexLapMap.CHUNK_SIDE - 1; x++) {
                for (int z = 0; z < HexLapMap.CHUNK_SIDE - 1; z++) {
                    ownPairs += 2;
                    ownBorders += own.getBiome(x, z).equals(own.getBiome(x + 1, z)) ? 0 : 1;
                    ownBorders += own.getBiome(x, z).equals(own.getBiome(x, z + 1)) ? 0 : 1;
                }
            }
        }

        double ownShare = (double) ownBorders / ownPairs;
        for (int side : TORUS_CHUNK_SIDES) {
            long lapBorders = 0;
            long lapPairs = 0;
            for (int chunk = 0; chunk < CHUNK_SAMPLES; chunk++) {
                HexLapChunk<WoverBiomePicker.PickableBiome> lap = new HexLapChunk<>(side, HexLapMap.CHUNK_SIDE,
                        false, false, 0, new WorldgenRandom(RandomSource.create(SEED + chunk)), picker);
                for (int x = 0; x < side - 1; x++) {
                    for (int z = 0; z < HexLapMap.CHUNK_SIDE - 1; z++) {
                        lapPairs += 2;
                        lapBorders += lap.get(x, z).equals(lap.get(x + 1, z)) ? 0 : 1;
                        lapBorders += lap.get(x, z).equals(lap.get(x, z + 1)) ? 0 : 1;
                    }
                }
            }

            double lapShare = (double) lapBorders / lapPairs;
            assertEquals(1.0, lapShare / ownShare, REGION_TOLERANCE, "neighbouring cells reading two biomes: the lap "
                    + "chunk " + side + " cells wide " + lapShare + " against WorldWeaver's " + ownShare);
        }
    }

    @Test
    void aChunkSeedsOneCellInSixtyFourWhateverItsSides() {
        assertEquals(16, HexLapChunk.seedCount(32, 32, false, false));
        assertEquals(14, HexLapChunk.seedCount(27, 32, false, false));
        assertEquals(14, HexLapChunk.seedCount(28, 32, false, false));
        assertEquals(19, HexLapChunk.seedCount(37, 32, false, false));
        assertEquals(4, HexLapChunk.seedCount(7, 7, true, true));
        assertEquals(2, HexLapChunk.seedCount(2, 32, true, false));
        assertEquals(1, HexLapChunk.seedCount(1, 1, true, true));
    }

    static void assertCell(LapAxis axis, double ownCellBlocks, int countStep) {
        double lap = axis.blocks().domainLength;
        double cell = axis.latticeBlocks();
        assertTrue(cell <= ownCellBlocks + 1.0E-9,
                "cell " + cell + " is wider than WorldWeaver's " + ownCellBlocks + " on a " + lap + "-block lap");
        int fewer = axis.cells() - countStep;
        assertTrue(fewer < 1 || lap / fewer > ownCellBlocks,
                axis.cells() + " cells on a " + lap + "-block lap: " + fewer + " would still fit " + ownCellBlocks);
    }

    private static void assertPeriodic(WorldFold fold, float scale) {
        assertPeriodic(fold, scale, ClimateScaleCompression.NO_COMPRESSION);
    }

    private static void assertPeriodic(WorldFold fold, float scale, double factor) {
        assertPeriodic(map(fold, scale, factor), fold);
    }

    static void assertPeriodic(LapMap<Integer> map, WorldFold fold) {
        WrapDomain xDomain = fold.blockDomain(Direction.Axis.X);
        WrapDomain zDomain = fold.blockDomain(Direction.Axis.Z);
        int xLap = xDomain.domainLength;
        int zLap = zDomain.domainLength;
        int xStride = Math.max(MIN_STRIDE_BLOCKS, xLap / SAMPLES_PER_LAP);
        int zStride = Math.max(MIN_STRIDE_BLOCKS, zLap / SAMPLES_PER_LAP);
        Set<Integer> seen = new HashSet<>();
        for (int x = xDomain.lowerBound - SEAM_STRIP_BLOCKS; x < xDomain.upperBound; x += xStride) {
            for (int z = zDomain.lowerBound - SEAM_STRIP_BLOCKS; z < zDomain.upperBound; z += zStride) {
                int biome = map.biomeAt(x, z);
                seen.add(biome);
                assertEquals(biome, map.biomeAt(x + xLap, z), "x " + x + " z " + z + " one lap away on X");
                assertEquals(biome, map.biomeAt(x, z + zLap), "x " + x + " z " + z + " one lap away on Z");
            }
        }

        assertTrue(seen.size() > 1, "the map laid one biome over the whole lap: " + seen);
    }
}
