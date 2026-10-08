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

class PreliminarySurfaceLevelTest {
    private static final int SEARCH_FLOOR = -64;

    private static final int SEARCH_TOP = 320;

    private static final double COLUMN_WEIGHT_X = 0.001;

    private static final double COLUMN_WEIGHT_Z = 0.002;

    private static final int GRID_STEP = 16;

    private static final int GRID_CELLS = 5;

    private static DensityFunction columnDensity(DensityFunction column) {
        return DensityFunctions.add(
                DensityFunctions.yClampedGradient(SEARCH_FLOOR, SEARCH_TOP, 1.0, -1.0), column);
    }

    private static double surfaceAt(DensityFunction density, int x, int z) {
        return new PreliminarySurfaceLevel(density).compute(new DensityFunction.SinglePointContext(x, 0, z));
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
                    columnDensity(DensityFunctions.flatCache(new CountingColumn())));
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
}
