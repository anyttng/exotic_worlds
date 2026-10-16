package com.exoticworlds.engine.noise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseSettings;

class PreliminarySurfaceLevelTest {
    private static final int OVERWORLD_MIN_Y = -64;

    private static final int OVERWORLD_HEIGHT = 384;

    private static final int OVERWORLD_TOP = OVERWORLD_MIN_Y + OVERWORLD_HEIGHT;

    private static final int SIZE_HORIZONTAL = 1;

    private static final int SIZE_VERTICAL = 2;

    private static final int FINE_SIZE_VERTICAL = 1;

    private static final int RAISED_HEIGHT = 512;

    private static final int LOWERED_MIN_Y = -128;

    private static final int LOWERED_HEIGHT = OVERWORLD_TOP - LOWERED_MIN_Y;

    private static final NoiseSettings OVERWORLD =
            NoiseSettings.create(OVERWORLD_MIN_Y, OVERWORLD_HEIGHT, SIZE_HORIZONTAL, SIZE_VERTICAL);

    private static final NoiseSettings RAISED =
            NoiseSettings.create(OVERWORLD_MIN_Y, RAISED_HEIGHT, SIZE_HORIZONTAL, SIZE_VERTICAL);

    private static final NoiseSettings LOWERED =
            NoiseSettings.create(LOWERED_MIN_Y, LOWERED_HEIGHT, SIZE_HORIZONTAL, SIZE_VERTICAL);

    private static final NoiseSettings FINE =
            NoiseSettings.create(OVERWORLD_MIN_Y, OVERWORLD_HEIGHT, SIZE_HORIZONTAL, FINE_SIZE_VERTICAL);

    private static final int LAND_EDGE_BLOCKS = 4;

    private static final int HIGH_LAND_Y = 400;

    private static final int DEEP_LAND_Y = -96;

    private static final int MID_LAND_Y = 100;

    private static final int OVERWORLD_WALK_AT_MID_LAND = 96;

    private static final double COLUMN_WEIGHT_X = 0.001;

    private static final double COLUMN_WEIGHT_Z = 0.002;

    private static final int GRID_STEP = 16;

    private static final int GRID_CELLS = 5;

    private static DensityFunction columnDensity(DensityFunction column) {
        return DensityFunctions.add(
                DensityFunctions.yClampedGradient(OVERWORLD_MIN_Y, OVERWORLD_TOP, 1.0, -1.0), column);
    }

    private static DensityFunction landUpTo(int y) {
        return DensityFunctions.yClampedGradient(y, y + LAND_EDGE_BLOCKS, 1.0, -1.0);
    }

    private static double surfaceAt(DensityFunction density, NoiseSettings bounds) {
        return new PreliminarySurfaceLevel(density, bounds).compute(new DensityFunction.SinglePointContext(0, 0, 0));
    }

    private static double surfaceAt(DensityFunction density, int x, int z) {
        return new PreliminarySurfaceLevel(density, OVERWORLD)
                .compute(new DensityFunction.SinglePointContext(x, 0, z));
    }

    private static final class CountingColumn implements DensityFunction.SimpleFunction {
        private final List<Integer> heightsRead = new ArrayList<>();

        @Override
        public double compute(FunctionContext context) {
            this.heightsRead.add(context.blockY());
            return context.blockX() * COLUMN_WEIGHT_X + context.blockZ() * COLUMN_WEIGHT_Z;
        }

        @Override
        public double minValue() {
            return 0.0;
        }

        @Override
        public double maxValue() {
            return GRID_STEP * GRID_CELLS * (COLUMN_WEIGHT_X + COLUMN_WEIGHT_Z);
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            throw new UnsupportedOperationException();
        }
    }

    @Nested
    class ASearchedColumn {
        @Test
        void samplesItsFlatCachedInputOnce() {
            CountingColumn column = new CountingColumn();
            surfaceAt(columnDensity(DensityFunctions.flatCache(column)), 0, 0);
            assertEquals(1, column.heightsRead.size());
        }

        @Test
        void samplesItsTwoDimensionalCachedInputOnce() {
            CountingColumn column = new CountingColumn();
            surfaceAt(columnDensity(DensityFunctions.cache2d(column)), 0, 0);
            assertEquals(1, column.heightsRead.size());
        }

        @Test
        void readsAFlatCachedInputAtHeightZeroAsNoiseChunkReadsIt() {
            CountingColumn column = new CountingColumn();
            surfaceAt(columnDensity(DensityFunctions.flatCache(column)), 0, 0);
            assertTrue(column.heightsRead.stream().allMatch(y -> y == 0));
        }
    }

    @Nested
    class TheSurfaceFound {
        private static void assertSameSurfaceOverTheGrid(UnaryOperator<DensityFunction> marker) {
            for (int cellX = 0; cellX < GRID_CELLS; cellX++) {
                for (int cellZ = 0; cellZ < GRID_CELLS; cellZ++) {
                    int x = cellX * GRID_STEP;
                    int z = cellZ * GRID_STEP;
                    assertEquals(surfaceAt(columnDensity(new CountingColumn()), x, z),
                            surfaceAt(columnDensity(marker.apply(new CountingColumn())), x, z),
                            "column x=" + x + " z=" + z);
                }
            }
        }

        @Test
        void isTheBareSearchsUnderAFlatCache() {
            assertSameSurfaceOverTheGrid(DensityFunctions::flatCache);
        }

        @Test
        void isTheBareSearchsUnderATwoDimensionalCache() {
            assertSameSurfaceOverTheGrid(DensityFunctions::cache2d);
        }

        @Test
        void followsEachColumnWhenOneSearchServesSeveral() {
            PreliminarySurfaceLevel shared = new PreliminarySurfaceLevel(
                    columnDensity(DensityFunctions.flatCache(new CountingColumn())), OVERWORLD);
            for (int cellX = 0; cellX < GRID_CELLS; cellX++) {
                for (int cellZ = 0; cellZ < GRID_CELLS; cellZ++) {
                    int x = cellX * GRID_STEP;
                    int z = cellZ * GRID_STEP;
                    assertEquals(surfaceAt(columnDensity(new CountingColumn()), x, z),
                            shared.compute(new DensityFunction.SinglePointContext(x, 0, z)),
                            "column x=" + x + " z=" + z);
                }
            }
        }
    }

    @Nested
    class TheSearchRange {
        @Test
        void keepsTheOverworldsEightBlockWalk() {
            assertEquals(OVERWORLD_WALK_AT_MID_LAND, surfaceAt(landUpTo(MID_LAND_Y), OVERWORLD));
        }

        @Test
        void reachesLandAboveTheOverworldsTopWhenTheGeneratorIsTaller() {
            assertEquals(HIGH_LAND_Y, surfaceAt(landUpTo(HIGH_LAND_Y), RAISED));
        }

        @Test
        void reachesLandBelowTheOverworldsFloorWhenTheGeneratorIsDeeper() {
            assertEquals(DEEP_LAND_Y, surfaceAt(landUpTo(DEEP_LAND_Y), LOWERED));
        }

        @Test
        void answersTheGeneratorsFloorForAColumnWithNoLand() {
            assertEquals(LOWERED_MIN_Y, surfaceAt(DensityFunctions.constant(-1.0), LOWERED));
        }

        @Test
        void walksInTheGeneratorsOwnCellHeight() {
            assertEquals(MID_LAND_Y, surfaceAt(landUpTo(MID_LAND_Y), FINE));
        }
    }
}
