package com.exoticworlds.compat.northbound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.exoticworlds.core.CoordinateConstants;
import com.exoticworlds.core.WorldLoopSizes;
import com.exoticworlds.core.WrapDomain;

import net.minecraft.world.level.levelgen.DensityFunction;

class LatitudeCoordinateTest {
    private static final WrapDomain MEDIUM = new WrapDomain(-1024, 1024);

    private static final int SAMPLE_Y = 64;

    private static final double NO_COMPRESSION = 1.0;

    private static final double[] FACTORS = {1.0, 2.0, 4.0, 16.0};

    private static final int[] PRESET_CHUNK_WIDTHS = {32, 64, 128, 256, 512};

    private static final double EPSILON = 1.0e-9;

    private static final double NORTHBOUND_OFFSET = 4096.0;

    private static final double LARGE_BIOMES_SCALE = 0.25;

    private static final double NORTHBOUND_FOLD = 524288.0;

    private static final int[] DEFAULT_OUTER_FOLDS = {-64, -32, -16, -8, -4, -2};

    private static final int[] LARGE_BIOMES_OUTER_FOLDS = {-16, -8, -4, -2};

    private static final double[] INNER_FOLDS = {-524288.0, -262144.0, -131072.0, -65536.0, -32768.0, -16384.0};

    private static final double NORTHBOUND_SCALE = 0.0001220703125;

    @ParameterizedTest
    @EnumSource(NorthboundLatitude.Variant.class)
    void handsNorthboundItsPoleAtTheSeamAndItsEquatorAtTheCentre(NorthboundLatitude.Variant variant) {
        LatitudeCoordinate coordinate = NorthboundLatitude.coordinate(MEDIUM, variant, NO_COMPRESSION);
        double seam = sample(coordinate, MEDIUM.lowerBound);
        double centre = sample(coordinate, MEDIUM.lowerBound + MEDIUM.domainLength / 2);

        assertEquals(1.0, northboundLatitude(seam, variant), EPSILON);
        assertEquals(0.0, northboundLatitude(centre, variant), EPSILON);
    }

    @ParameterizedTest
    @EnumSource(NorthboundLatitude.Variant.class)
    void keepsTheLatitudeInOneHemisphereAndContinuousAcrossTheSeam(NorthboundLatitude.Variant variant) {
        LatitudeCoordinate coordinate = NorthboundLatitude.coordinate(MEDIUM, variant, NO_COMPRESSION);
        double largestInside = 0.0;
        double previous = northboundLatitude(sample(coordinate, MEDIUM.lowerBound), variant);
        for (int z = MEDIUM.lowerBound + 1; z < MEDIUM.upperBound; z++) {
            double latitude = northboundLatitude(sample(coordinate, z), variant);
            assertTrue(latitude >= -EPSILON && latitude <= 1.0 + EPSILON, "latitude " + latitude + " at z=" + z);
            largestInside = Math.max(largestInside, Math.abs(latitude - previous));
            previous = latitude;
        }

        double acrossSeam = Math.abs(northboundLatitude(sample(coordinate, MEDIUM.lowerBound), variant)
                - northboundLatitude(sample(coordinate, MEDIUM.lowerBound - 1), variant));
        assertTrue(acrossSeam <= largestInside + EPSILON, acrossSeam + " across the seam, " + largestInside + " inside");
    }

    @ParameterizedTest
    @EnumSource(NorthboundLatitude.Variant.class)
    void agreesOneLapAway(NorthboundLatitude.Variant variant) {
        LatitudeCoordinate coordinate = NorthboundLatitude.coordinate(MEDIUM, variant, NO_COMPRESSION);
        for (int z = MEDIUM.lowerBound - MEDIUM.domainLength; z < MEDIUM.upperBound; z += 7) {
            assertEquals(sample(coordinate, z), sample(coordinate, z + MEDIUM.domainLength));
        }
    }

    @Test
    void layOneBandOnEveryPresetWithoutCompression() {
        for (int chunkWidth : PRESET_CHUNK_WIDTHS) {
            int lap = chunkWidth * CoordinateConstants.CHUNK_WIDTH;
            for (NorthboundLatitude.Variant variant : NorthboundLatitude.Variant.values()) {
                assertEquals(1, NorthboundLatitude.bands(lap, variant.poleToEquatorBlocks()), "lap " + lap);
            }
        }
    }

    @Test
    void layTheFewestOddBandsNoWiderThanTheCap() {
        for (int chunkWidth = WorldLoopSizes.MIN_CHUNK_WIDTH; chunkWidth <= WorldLoopSizes.MAX_CHUNK_WIDTH;
                chunkWidth = chunkWidth * 3 / 2) {
            int lap = chunkWidth * CoordinateConstants.CHUNK_WIDTH;
            for (NorthboundLatitude.Variant variant : NorthboundLatitude.Variant.values()) {
                for (double factor : FACTORS) {
                    double cap = variant.poleToEquatorBlocks() / factor;
                    int bands = NorthboundLatitude.bands(lap, cap);
                    String reading = "lap " + lap + ", cap " + cap + ", bands " + bands;
                    assertEquals(1, bands % 2, reading);
                    assertTrue(lap / (2.0 * bands) <= cap, reading);
                    assertTrue(bands == 1 || lap / (2.0 * (bands - 2)) > cap, reading);
                }
            }
        }
    }

    private static double sample(LatitudeCoordinate coordinate, int z) {
        return coordinate.compute(new DensityFunction.SinglePointContext(0, SAMPLE_Y, z));
    }

    private static double northboundLatitude(double northboundZ, NorthboundLatitude.Variant variant) {
        boolean large = variant == NorthboundLatitude.Variant.LARGE_BIOMES;
        double value = (large ? northboundZ * LARGE_BIOMES_SCALE : northboundZ) + NORTHBOUND_OFFSET;
        for (int fold : large ? LARGE_BIOMES_OUTER_FOLDS : DEFAULT_OUTER_FOLDS) {
            value = Math.abs(value) + NORTHBOUND_FOLD * fold;
        }

        for (double fold : INNER_FOLDS) {
            value = Math.abs(value) + fold;
        }

        return Math.abs(value) * NORTHBOUND_SCALE - 1.0;
    }
}
