package com.exoticworlds.compat.naturaltemperature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WrapDomain;

import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

class ClimateLapTest {
    private static final int LAP = 8192;

    private static final int LAP_CHUNKS = LAP / 16;

    private static final int SKEW_CHUNKS = 96;

    private static final WrapDomain LAP_DOMAIN = new WrapDomain(-LAP / 2, LAP / 2);

    private static final WrapDomain OFF_CENTER_DOMAIN = new WrapDomain(-1000, LAP - 1000);

    private static final TranslationLattice TORUS = TranslationLattice.unskewed(LAP_DOMAIN, LAP_DOMAIN);

    private static final TranslationLattice SKEWED = WorldFolds.of(FlatShape.latticeTorus(
            ShapeFixtures.loopedBounds(-LAP_CHUNKS / 2, LAP_CHUNKS / 2, -LAP_CHUNKS / 2, LAP_CHUNKS / 2), SKEW_CHUNKS))
            .blockLattice();

    private static final TranslationLattice CYLINDER_X = TranslationLattice.unskewed(LAP_DOMAIN, new WrapDomain.Noop());

    private static final TranslationLattice CYLINDER_Z = TranslationLattice.unskewed(new WrapDomain.Noop(), LAP_DOMAIN);

    private static final TranslationLattice OFF_CENTER = TranslationLattice.unskewed(OFF_CENTER_DOMAIN, OFF_CENTER_DOMAIN);

    private static final List<TranslationLattice> LATTICES = List.of(TORUS, SKEWED, CYLINDER_X, CYLINDER_Z, OFF_CENTER);

    private static final double[] COMPRESSIONS = {1.0, 1.5, 2.0, 3.7, 8.0};

    private static final List<TranslationLattice> LOOPED_Z = List.of(TORUS, SKEWED, CYLINDER_Z, OFF_CENTER);

    private static final int[] MODES = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};

    private static final int[] LATITUDE_MODES = {0, 1, 2, 5, 6, 11, 12};

    private static final int[] RADIAL_MODES = {3, 4, 7, 8, 9, 10};

    private static final int RADIAL_MODE = 3;

    private static final int[] NOISE_MODES = {0, RADIAL_MODE};

    private static final double SIDE_WAVE_RATE = 0.003;

    private static final int EVEN_SPAN = 64;

    private static final int ROUNDING_SLACK = 2;

    private static final double[] EQUATORIAL_DISTANCES = {50000.0, 2048.0, 700.0, 100.0};

    private static final double EQUATOR_OFFSET = 1234.0;

    private static final int[] CROSS = {-3000, -1, 0, 517, 3999};

    private static final int[] OWN_SCALE_REACH = {1, 517, 3000, LAP / 2};

    private static final int WIDE_LAP_DISTANCE = 700;

    private static final int NARROW_LAP_DISTANCE = 50000;

    private static final boolean[] LOOPING_WORLD = {true, false};

    private static final int[] SIDE_REACH = {1, 517, 3000};

    private static final double[] WAVE_RATES = {0.0005, 0.0007, 0.003, 0.00001};

    private static final double[] WAVE_LAPS = {LAP, LAP * 1.5, 512.0};

    private static final double WAVE_AMPLITUDE = 84.0;

    private static final long WAVE_SEED = 8891L;

    private static final int WAVE_SAMPLES = 257;

    private static final double WAVE_NUDGE = 1.0E-4;

    private static final double WAVE_STEP_SLACK = 1.0E-2;

    private static final WaveCells CELLS = new WaveCells() {
        @Override
        public double pointShare(long seed, long cell) {
            return 0.25 + 0.5 * new Random(seed * 31 + cell * 17 + 1).nextDouble();
        }

        @Override
        public double value(long seed, long cell) {
            return 2.0 * new Random(seed * 31 + cell * 17 + 2).nextDouble() - 1.0;
        }
    };

    private static final double DETAIL_SCALE = 6.0;

    private static final long NOISE_SEED = 42L;

    private static final int NARROW_LAP = 512;

    private static final int WIDE_LAP = 65536;

    private static final int WIDE_STRIDE = 31;

    private static final double STEEPER_SLACK = 1.25;

    private static final double FLATTER_SLACK = 3.0;

    private static final double TURN_SLACK = 1.0E-6;

    private static final double WAVE = 37.5;

    private static final double DISTORTION = -11.25;

    private static final double FOLD_SLACK = 1.0E-6;

    private static final double[] BAND_WIDTHS = {100.0, 500.0, 2500.0, 40000.0};

    private static final double NOISE_CELLS_PER_BLOCK = 0.046875;

    private static final double NOISE_SLACK = 1.0E-5;

    @Test
    void xTakesTheSameValueOneLapAwayOnEveryLattice() {
        for (TranslationLattice lattice : LATTICES) {
            if (!lattice.x().loops()) {
                continue;
            }

            for (double compression : COMPRESSIONS) {
                ClimateLap lap = new ClimateLap(lattice, compression);
                for (int mode : MODES) {
                    for (double distance : EQUATORIAL_DISTANCES) {
                        for (int z : CROSS) {
                            for (int x = lattice.x().lowerBound - 3; x < lattice.x().lowerBound + 3; x++) {
                                assertEquals(lap.x(x, z, mode, distance, true),
                                        lap.x(x + lattice.x().domainLength, z, mode, distance, true));
                                if (lattice.z().loops()) {
                                    assertEquals(lap.x(x, z, mode, distance, true), lap.x(x + lattice.skew(),
                                            z + lattice.z().domainLength, mode, distance, true));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    void xStepsAcrossTheSeamAsBesideIt() {
        for (TranslationLattice lattice : LATTICES) {
            if (!lattice.x().loops()) {
                continue;
            }

            for (double compression : COMPRESSIONS) {
                ClimateLap lap = new ClimateLap(lattice, compression);
                for (int mode : MODES) {
                    for (double distance : EQUATORIAL_DISTANCES) {
                        int step = (int) Math.ceil(lap.xScale(mode, distance, true));
                        double turn = ClimateLap.radial(mode) ? 0.0
                                : lattice.x().domainLength * lap.xScale(mode, distance, true);
                        for (int z : CROSS) {
                            int lower = lattice.x().lowerBound;
                            int upper = lower + lattice.x().domainLength;
                            int jump = Math.abs(lap.x(lower, z, mode, distance, true)
                                    - lap.x(upper - 1, z, mode, distance, true));
                            assertTrue(jump <= step || Math.abs(jump - turn) <= step + 1, "jump " + jump);
                            assertTrue(Math.abs(lap.x(upper - 1, z, mode, distance, true)
                                    - lap.x(upper - 2, z, mode, distance, true)) <= step);
                        }
                    }
                }
            }
        }
    }

    @Test
    void xAdvancesAsTheLatitudeOnALoopedZ() {
        for (TranslationLattice lattice : LOOPED_Z) {
            WrapDomain x = lattice.x();
            WrapDomain z = lattice.z();
            int fromX = x.loops() ? x.lowerBound + x.domainLength / 2 : 0;
            int atZ = z.lowerBound + z.domainLength / 2;
            for (double compression : COMPRESSIONS) {
                ClimateLap lap = new ClimateLap(lattice, compression);
                for (int mode : LATITUDE_MODES) {
                    for (double distance : EQUATORIAL_DISTANCES) {
                        int span = Math.min(EVEN_SPAN, z.domainLength / (2 * lap.bands(distance)));
                        int alongZ = Math.abs(lap.z(z.lowerBound + span, mode, distance, EQUATOR_OFFSET, true)
                                - lap.z(z.lowerBound, mode, distance, EQUATOR_OFFSET, true));
                        int alongX = Math.abs(lap.x(fromX + span, atZ, mode, distance, true)
                                - lap.x(fromX, atZ, mode, distance, true));
                        assertTrue(Math.abs(alongZ - alongX) <= ROUNDING_SLACK,
                                "z " + alongZ + " x " + alongX + " over " + span);
                    }
                }
            }
        }
    }

    @Test
    void zTakesTheSameValueOneLapAwayInEveryMode() {
        for (TranslationLattice lattice : LATTICES) {
            if (!lattice.z().loops()) {
                continue;
            }

            for (double compression : COMPRESSIONS) {
                ClimateLap lap = new ClimateLap(lattice, compression);
                for (int mode : MODES) {
                    for (double distance : EQUATORIAL_DISTANCES) {
                        for (int z = lattice.z().lowerBound - 3; z < lattice.z().lowerBound + 3; z++) {
                            assertEquals(lap.z(z, mode, distance, EQUATOR_OFFSET, true),
                                    lap.z(z + lattice.z().domainLength, mode, distance, EQUATOR_OFFSET, true));
                        }
                    }
                }
            }
        }
    }

    @Test
    void latitudeStepsAcrossTheSeamAsBesideIt() {
        for (TranslationLattice lattice : LATTICES) {
            if (!lattice.z().loops()) {
                continue;
            }

            for (double compression : COMPRESSIONS) {
                ClimateLap lap = new ClimateLap(lattice, compression);
                for (double distance : EQUATORIAL_DISTANCES) {
                    int lower = lattice.z().lowerBound;
                    int upper = lower + lattice.z().domainLength;
                    int seam = Math.abs(lap.z(lower, 0, distance, EQUATOR_OFFSET, true)
                            - lap.z(upper - 1, 0, distance, EQUATOR_OFFSET, true));
                    int beside = Math.abs(lap.z(upper - 1, 0, distance, EQUATOR_OFFSET, true)
                            - lap.z(upper - 2, 0, distance, EQUATOR_OFFSET, true));
                    assertTrue(seam <= beside + 1, "seam " + seam + " beside " + beside);
                }
            }
        }
    }

    @Test
    void latitudeHasAPoleAtTheSeamAndTheEquatorAtTheCenter() {
        for (double compression : COMPRESSIONS) {
            ClimateLap lap = new ClimateLap(TORUS, compression);
            for (double distance : EQUATORIAL_DISTANCES) {
                assertEquals(Math.round(-distance - EQUATOR_OFFSET), lap.z(LAP_DOMAIN.lowerBound, 0, distance,
                        EQUATOR_OFFSET, true));
                assertEquals(Math.round(distance - EQUATOR_OFFSET), lap.z(0, 0, distance, EQUATOR_OFFSET, true));
            }
        }
    }

    @Test
    void bandsAreTheFewestOddCountNoWiderThanTheCompressedSpan() {
        for (double compression : COMPRESSIONS) {
            ClimateLap lap = new ClimateLap(TORUS, compression);
            for (double distance : EQUATORIAL_DISTANCES) {
                int bands = lap.bands(distance);
                double capped = 2.0 * distance / compression;
                assertEquals(1, bands % 2);
                assertTrue(LAP / (2.0 * bands) <= capped, bands + " bands, cap " + capped);
                assertTrue(bands == 1 || LAP / (2.0 * (bands - 2)) > capped, bands + " bands, cap " + capped);
            }
        }
    }

    @Test
    void radialModesKeepTheDistanceFromTheLapCenter() {
        for (double compression : COMPRESSIONS) {
            ClimateLap lap = new ClimateLap(TORUS, compression);
            for (int z : CROSS) {
                assertEquals(Math.round(Math.abs(z) * compression), lap.z(z, 3, 50000.0, EQUATOR_OFFSET, true));
                assertEquals(Math.round(Math.abs(z) * compression), lap.x(z, 0, RADIAL_MODE, 50000.0, true));
            }
        }
    }

    @Test
    void radialModesKeepTheSideOfTheLapCenterForTheDisplacement() {
        for (TranslationLattice lattice : List.of(TORUS, SKEWED, CYLINDER_X, OFF_CENTER)) {
            int center = lattice.x().lowerBound + lattice.x().domainLength / 2;
            for (double compression : COMPRESSIONS) {
                ClimateLap lap = new ClimateLap(lattice, compression);
                for (int mode : RADIAL_MODES) {
                    assertSignedX(lap, lattice.x(), center, mode);
                }
            }
        }
    }

    private static void assertSignedX(ClimateLap lap, WrapDomain x, int center, int mode) {
        double handedLap = x.domainLength * lap.xScale(mode, 50000.0, true);
        int differing = 0;
        for (int fromCenter : SIDE_REACH) {
            int east = lap.signedX(center + fromCenter, 0, mode, 50000.0, true);
            int west = lap.signedX(center - fromCenter, 0, mode, 50000.0, true);
            assertTrue(east > 0 && west < 0, "east " + east + " west " + west);
            assertEquals(east, lap.x(center + fromCenter, 0, mode, 50000.0, true));
            assertEquals(-west, lap.x(center - fromCenter, 0, mode, 50000.0, true), ROUNDING_SLACK);
            assertEquals(east, lap.signedX(center + fromCenter + x.domainLength, 0, mode, 50000.0, true));
            double eastWave = ClimateLap.wave(CELLS, WAVE_SEED, east, handedLap, WAVE_AMPLITUDE, SIDE_WAVE_RATE);
            double westWave = ClimateLap.wave(CELLS, WAVE_SEED, west, handedLap, WAVE_AMPLITUDE, SIDE_WAVE_RATE);
            differing += Math.abs(eastWave - westWave) > FOLD_SLACK ? 1 : 0;
        }

        assertTrue(differing > 0, "the wave mirrors about the lap center");
    }

    @Test
    void anOpenAxisShrinksByTheCompression() {
        for (double compression : COMPRESSIONS) {
            ClimateLap cylinderX = new ClimateLap(CYLINDER_X, compression);
            ClimateLap cylinderZ = new ClimateLap(CYLINDER_Z, compression);
            for (int coord : CROSS) {
                for (int mode : MODES) {
                    assertEquals(Math.round(coord * compression), cylinderX.z(coord, mode, 50000.0, EQUATOR_OFFSET, true));
                }

                assertEquals(Math.round(coord * compression), cylinderZ.x(coord, 0, RADIAL_MODE, 50000.0, true));
                for (int mode : MODES) {
                    assertEquals(Math.round((ClimateLap.radial(mode) ? Math.abs(coord) : coord) * compression),
                            cylinderX.x(coord, 0, mode, 50000.0, true));
                }
            }
        }
    }

    @Test
    void noEquatorialDistanceKeepsTheBlock() {
        ClimateLap lap = new ClimateLap(TORUS, 1.0);
        for (int z : CROSS) {
            assertEquals(z, lap.z(z, 0, 0.0, EQUATOR_OFFSET, true));
        }
    }

    @Test
    void ownScaleKeepsTheMirroredDistanceFromTheLapCenter() {
        for (TranslationLattice lattice : LOOPED_Z) {
            for (double compression : COMPRESSIONS) {
                assertOwnScaleZ(new ClimateLap(lattice, compression), lattice.z(), compression);
            }
        }
    }

    private static void assertOwnScaleZ(ClimateLap lap, WrapDomain z, double compression) {
        int center = z.lowerBound + z.domainLength / 2;
        for (int mode : LATITUDE_MODES) {
            for (double distance : EQUATORIAL_DISTANCES) {
                assertEquals(0, lap.z(center, mode, distance, EQUATOR_OFFSET, false));
                for (int fromCenter : OWN_SCALE_REACH) {
                    int expected = (int) -Math.round(fromCenter * compression);
                    assertEquals(expected, lap.z(center + fromCenter, mode, distance, EQUATOR_OFFSET, false));
                    assertEquals(expected, lap.z(center - fromCenter, mode, distance, EQUATOR_OFFSET, false));
                    assertEquals(expected,
                            lap.z(center + fromCenter + z.domainLength, mode, distance, EQUATOR_OFFSET, false));
                }
            }
        }
    }

    @Test
    void ownScaleReachesTheModsPoleOnlyOnALapWiderThanTheBand() {
        ClimateLap lap = new ClimateLap(TORUS, 1.0);
        int edge = LAP_DOMAIN.lowerBound;
        assertEquals(-WIDE_LAP_DISTANCE, lap.z(WIDE_LAP_DISTANCE, 0, WIDE_LAP_DISTANCE, 0.0, false));
        assertEquals(-WIDE_LAP_DISTANCE, lap.z(-WIDE_LAP_DISTANCE, 0, WIDE_LAP_DISTANCE, 0.0, false));
        assertEquals(-2 * WIDE_LAP_DISTANCE, lap.z(2 * WIDE_LAP_DISTANCE, 0, WIDE_LAP_DISTANCE, 0.0, false));
        assertEquals(-LAP / 2, lap.z(edge, 0, WIDE_LAP_DISTANCE, 0.0, false));
        assertEquals(-LAP / 2, lap.z(edge, 0, NARROW_LAP_DISTANCE, 0.0, false));
        assertTrue(lap.z(edge, 0, WIDE_LAP_DISTANCE, 0.0, false) <= -WIDE_LAP_DISTANCE);
        assertTrue(lap.z(edge, 0, NARROW_LAP_DISTANCE, 0.0, false) > -NARROW_LAP_DISTANCE);
    }

    @Test
    void ownScaleStepsAcrossTheZSeamAsBesideIt() {
        for (TranslationLattice lattice : LOOPED_Z) {
            int lower = lattice.z().lowerBound;
            int upper = lower + lattice.z().domainLength;
            for (double compression : COMPRESSIONS) {
                ClimateLap lap = new ClimateLap(lattice, compression);
                int seam = Math.abs(lap.z(lower, 0, 50000.0, EQUATOR_OFFSET, false)
                        - lap.z(upper - 1, 0, 50000.0, EQUATOR_OFFSET, false));
                assertTrue(seam <= Math.ceil(compression), "seam " + seam);
            }
        }
    }

    @Test
    void ownScaleMovesBothAxesByTheCompression() {
        for (TranslationLattice lattice : LOOPED_Z) {
            for (double compression : COMPRESSIONS) {
                assertOwnScaleAdvance(new ClimateLap(lattice, compression), lattice, compression);
            }
        }
    }

    private static void assertOwnScaleAdvance(ClimateLap lap, TranslationLattice lattice, double compression) {
        WrapDomain x = lattice.x();
        WrapDomain z = lattice.z();
        int fromX = x.loops() ? x.lowerBound + x.domainLength / 2 : 0;
        int atZ = z.lowerBound + z.domainLength / 2;
        long expected = Math.round(EVEN_SPAN * compression);
        for (int mode : LATITUDE_MODES) {
            for (double distance : EQUATORIAL_DISTANCES) {
                assertEquals(compression, lap.xScale(mode, distance, false));
                int alongZ = Math.abs(lap.z(atZ + EVEN_SPAN, mode, distance, EQUATOR_OFFSET, false)
                        - lap.z(atZ, mode, distance, EQUATOR_OFFSET, false));
                int alongX = Math.abs(lap.x(fromX + EVEN_SPAN, atZ, mode, distance, false)
                        - lap.x(fromX, atZ, mode, distance, false));
                assertTrue(Math.abs(alongZ - expected) <= ROUNDING_SLACK, "z " + alongZ + " for " + expected);
                assertTrue(Math.abs(alongX - expected) <= ROUNDING_SLACK, "x " + alongX + " for " + expected);
            }
        }
    }

    @Test
    void ownScaleXTakesTheSameValueOneLapAway() {
        for (TranslationLattice lattice : List.of(TORUS, SKEWED, OFF_CENTER)) {
            for (double compression : COMPRESSIONS) {
                assertOwnScaleXLaps(new ClimateLap(lattice, compression), lattice);
            }
        }
    }

    private static void assertOwnScaleXLaps(ClimateLap lap, TranslationLattice lattice) {
        int lower = lattice.x().lowerBound;
        for (int z : CROSS) {
            for (int x = lower - 3; x < lower + 3; x++) {
                int here = lap.x(x, z, 0, 50000.0, false);
                assertEquals(here, lap.x(x + lattice.x().domainLength, z, 0, 50000.0, false));
                assertEquals(here, lap.x(x + lattice.skew(), z + lattice.z().domainLength, 0, 50000.0, false));
            }
        }
    }

    @Test
    void xKeepsItsSideOfTheLapCenterOutsideRadialModes() {
        for (TranslationLattice lattice : List.of(TORUS, CYLINDER_X, OFF_CENTER)) {
            int center = lattice.x().lowerBound + lattice.x().domainLength / 2;
            for (double compression : COMPRESSIONS) {
                ClimateLap lap = new ClimateLap(lattice, compression);
                for (int mode : LATITUDE_MODES) {
                    for (boolean looping : LOOPING_WORLD) {
                        for (int fromCenter : SIDE_REACH) {
                            int east = lap.x(center + fromCenter, 0, mode, 50000.0, looping);
                            int west = lap.x(center - fromCenter, 0, mode, 50000.0, looping);
                            assertTrue(east != west, "mirrored at " + fromCenter);
                        }
                    }
                }
            }
        }
    }

    @Test
    void theWaveRepeatsOnTheLapWithoutAStep() {
        for (double lap : WAVE_LAPS) {
            for (double rate : WAVE_RATES) {
                assertWaveOnTheLap(lap, rate);
            }
        }
    }

    private static void assertWaveOnTheLap(double lap, double rate) {
        double lowest = Double.MAX_VALUE;
        double highest = -Double.MAX_VALUE;
        for (int sample = 0; sample <= WAVE_SAMPLES; sample++) {
            double position = -lap / 2.0 + lap * sample / WAVE_SAMPLES;
            double here = ClimateLap.wave(CELLS, WAVE_SEED, position, lap, WAVE_AMPLITUDE, rate);
            assertEquals(here, ClimateLap.wave(CELLS, WAVE_SEED, position + lap, lap, WAVE_AMPLITUDE, rate),
                    FOLD_SLACK);
            assertEquals(here, ClimateLap.wave(CELLS, WAVE_SEED, position + WAVE_NUDGE, lap, WAVE_AMPLITUDE, rate),
                    WAVE_STEP_SLACK);
            assertTrue(Math.abs(here) <= WAVE_AMPLITUDE, "wave " + here);
            lowest = Math.min(lowest, here);
            highest = Math.max(highest, here);
        }

        assertEquals(rate * lap > 1.0, highest - lowest > FOLD_SLACK, "span " + (highest - lowest));
    }

    @Test
    void theWaveIsFlatWithNoAmplitudeOrRate() {
        assertEquals(0.0, ClimateLap.wave(CELLS, WAVE_SEED, 517.0, LAP, 0.0, 0.0005));
        assertEquals(0.0, ClimateLap.wave(CELLS, WAVE_SEED, 517.0, LAP, WAVE_AMPLITUDE, 0.0));
    }

    @Test
    void theSixZoneCirclesAreRadialAndTheDiscreteModesFollowTheLatitude() {
        for (int mode : RADIAL_MODES) {
            assertTrue(ClimateLap.radial(mode), "mode " + mode);
        }

        for (int mode : LATITUDE_MODES) {
            assertTrue(!ClimateLap.radial(mode), "mode " + mode);
        }
    }

    @Test
    void theLatitudeFoldsAfterTheDisplacement() {
        for (TranslationLattice lattice : LOOPED_Z) {
            for (double compression : COMPRESSIONS) {
                ClimateLap lap = new ClimateLap(lattice, compression);
                for (double distance : EQUATORIAL_DISTANCES) {
                    for (boolean looping : LOOPING_WORLD) {
                        assertFoldAfterDisplacement(lap, lattice.z(), distance, looping);
                    }
                }
            }
        }
    }

    private static void assertFoldAfterDisplacement(ClimateLap lap, WrapDomain z, double distance, boolean looping) {
        double scale = lap.xScale(0, distance, looping);
        for (int blockZ : CROSS) {
            int handed = lap.z(blockZ, 0, distance, EQUATOR_OFFSET, looping);
            double carried = lap.carriedWave(blockZ, handed, WAVE, DISTORTION, 0, distance, EQUATOR_OFFSET, looping);
            double displaced = lap.latitude(blockZ + (DISTORTION + WAVE) / scale, distance, EQUATOR_OFFSET, looping);
            assertEquals(displaced, handed + DISTORTION + carried, FOLD_SLACK);
            assertEquals(carried, lap.carriedWave(blockZ + z.domainLength, handed, WAVE, DISTORTION, 0, distance,
                    EQUATOR_OFFSET, looping), FOLD_SLACK);
        }
    }

    @Test
    void aDisplacementMovesBothSidesOfTheCenterTheSameWay() {
        ClimateLap lap = new ClimateLap(TORUS, 1.0);
        for (boolean looping : LOOPING_WORLD) {
            double north = lap.latitude(-EVEN_SPAN + WAVE, 2048.0, 0.0, looping)
                    - lap.latitude(-EVEN_SPAN, 2048.0, 0.0, looping);
            double south = lap.latitude(EVEN_SPAN + WAVE, 2048.0, 0.0, looping)
                    - lap.latitude(EVEN_SPAN, 2048.0, 0.0, looping);
            assertEquals(-north, south, FOLD_SLACK);
            assertTrue(Math.abs(north) > FOLD_SLACK);
        }
    }

    @Test
    void theWaveIsLeftAloneOnAnOpenZAndInRadialModes() {
        assertEquals(WAVE, new ClimateLap(CYLINDER_X, 2.0).carriedWave(517, 1034, WAVE, DISTORTION, 0, 50000.0,
                EQUATOR_OFFSET, true));
        assertEquals(WAVE, new ClimateLap(TORUS, 2.0).carriedWave(517, 1034, WAVE, DISTORTION, RADIAL_MODE, 50000.0,
                EQUATOR_OFFSET, true));
    }

    @Test
    void humidityBandsRepeatWholeTimesOnTheXLapAndNeverWider() {
        for (TranslationLattice lattice : List.of(TORUS, SKEWED, CYLINDER_X, OFF_CENTER)) {
            for (double compression : COMPRESSIONS) {
                ClimateLap lap = new ClimateLap(lattice, compression);
                for (int mode : MODES) {
                    assertBandsOnTheLap(lap, lattice.x(), mode);
                }
            }
        }

        assertEquals(500.0, new ClimateLap(CYLINDER_Z, 2.0).lapBandWidth(500.0, 0, 50000.0, true));
    }

    private static void assertBandsOnTheLap(ClimateLap lap, WrapDomain x, int mode) {
        double handedLap = x.domainLength * lap.xScale(mode, 50000.0, true);
        for (double width : BAND_WIDTHS) {
            double fitted = lap.lapBandWidth(width, mode, 50000.0, true);
            double repeats = handedLap / (4.0 * fitted);
            assertEquals(Math.rint(repeats), repeats, TURN_SLACK);
            assertTrue(fitted <= width + TURN_SLACK, fitted + " over " + width);
        }
    }

    @Test
    void theNoiseRepeatsOnTheLapAndNotAboutItsCenter() {
        byte[] permutations = permutations();
        for (TranslationLattice lattice : LATTICES) {
            for (double compression : COMPRESSIONS) {
                for (int mode : NOISE_MODES) {
                    assertNoiseOnTheLap(new ClimateLap(lattice, compression), lattice, permutations, mode);
                }
            }
        }
    }

    private static void assertNoiseOnTheLap(ClimateLap lap, TranslationLattice lattice, byte[] permutations,
            int mode) {
        int lapX = lattice.x().loops() ? lattice.x().domainLength : 0;
        int lapZ = lattice.z().loops() ? lattice.z().domainLength : 0;
        int differing = 0;
        for (int x : CROSS) {
            for (int z : CROSS) {
                double here = noiseAt(lap, permutations, x, z, mode);
                assertEquals(here, noiseAt(lap, permutations, x + lapX, z, mode), NOISE_SLACK);
                assertEquals(here, noiseAt(lap, permutations, x + lattice.skew(), z + lapZ, mode), NOISE_SLACK);
                int mirrorX = 2 * (lattice.x().lowerBound + lapX / 2) - x;
                int mirrorZ = 2 * (lattice.z().lowerBound + lapZ / 2) - z;
                if (lapX != 0 && Math.abs(here - noiseAt(lap, permutations, mirrorX, z, mode)) > NOISE_SLACK) {
                    differing++;
                }

                if (lapZ != 0 && Math.abs(here - noiseAt(lap, permutations, x, mirrorZ, mode)) > NOISE_SLACK) {
                    differing++;
                }
            }
        }

        assertTrue(differing > CROSS.length, differing + " mirror pairs differ");
    }

    private static double noiseAt(ClimateLap lap, byte[] permutations, int blockX, int blockZ, int mode) {
        double scale = lap.xScale(mode, 50000.0, true);
        return lap.noise(permutations, 3.25, 7.5, 11.75, NOISE_CELLS_PER_BLOCK,
                lap.signedX(blockX, blockZ, mode, 50000.0, true), lap.z(blockZ, mode, 50000.0, EQUATOR_OFFSET, true),
                blockZ, scale, lap.shrink(NOISE_CELLS_PER_BLOCK, scale));
    }

    @Test
    void aNoiseStackRepeatsOnTheLapLayerByLayer() {
        NormalNoise stack = NormalNoise.create(new LegacyRandomSource(NOISE_SEED), -7, 1.0, 1.0);
        for (TranslationLattice lattice : LATTICES) {
            ClimateLap lap = new ClimateLap(lattice, 1.5);
            int lapX = lattice.x().loops() ? lattice.x().domainLength : 0;
            int lapZ = lattice.z().loops() ? lattice.z().domainLength : 0;
            double span = 0.0;
            for (int x : CROSS) {
                for (int z : CROSS) {
                    double here = stackAt(lap, stack, x, z);
                    assertEquals(here, stackAt(lap, stack, x + lapX, z), NOISE_SLACK);
                    assertEquals(here, stackAt(lap, stack, x + lattice.skew(), z + lapZ), NOISE_SLACK);
                    span = Math.max(span, Math.abs(here));
                }
            }

            assertTrue(span > NOISE_SLACK, "span " + span);
        }
    }

    @Test
    void aNoiseStackRepeatsOneHandedLapAlongX() {
        NormalNoise large = NormalNoise.create(new LegacyRandomSource(NOISE_SEED), -14, -14.0, -14.0, -14.0);
        NormalNoise detail = NormalNoise.create(new LegacyRandomSource(NOISE_SEED), -7, 1.0);
        for (TranslationLattice lattice : List.of(TORUS, SKEWED, CYLINDER_X, OFF_CENTER)) {
            for (double distance : EQUATORIAL_DISTANCES) {
                ClimateLap lap = new ClimateLap(lattice, 1.5);
                double scale = lap.xScale(0, distance, true);
                double handedLap = lattice.x().domainLength * scale;
                for (int z : CROSS) {
                    for (NormalNoise stack : List.of(large, detail)) {
                        double here = lap.noise(stack, DETAIL_SCALE, -handedLap / 2.0, 0.0, z, scale);
                        assertEquals(here, lap.noise(stack, DETAIL_SCALE, handedLap / 2.0, 0.0, z, scale),
                                NOISE_SLACK * Math.max(1.0, Math.abs(here)));
                    }
                }
            }
        }
    }

    @Test
    void aSqueezedNoiseStackKeepsItsMeanSlope() {
        NormalNoise large = NormalNoise.create(new LegacyRandomSource(NOISE_SEED), -14, -14.0, -14.0, -14.0);
        WrapDomain narrow = new WrapDomain(-NARROW_LAP / 2, NARROW_LAP / 2);
        WrapDomain wide = new WrapDomain(-WIDE_LAP / 2, WIDE_LAP / 2);
        double squeezed = meanSlope(new ClimateLap(TranslationLattice.unskewed(narrow, narrow), 1.0), large,
                NARROW_LAP, 1);
        double shipped = meanSlope(new ClimateLap(TranslationLattice.unskewed(wide, wide), 1.0), large, WIDE_LAP,
                WIDE_STRIDE);
        assertTrue(squeezed < STEEPER_SLACK * shipped && shipped < FLATTER_SLACK * squeezed,
                "squeezed " + squeezed + " shipped " + shipped);
    }

    private static double meanSlope(ClimateLap lap, NormalNoise stack, int lapBlocks, int stride) {
        double total = 0.0;
        int pairs = 0;
        for (int z = -lapBlocks / 2; z < lapBlocks / 2 - 1; z += stride) {
            total += Math.abs(lap.noise(stack, DETAIL_SCALE, 0.0, 0.0, z + 1, 1.0)
                    - lap.noise(stack, DETAIL_SCALE, 0.0, 0.0, z, 1.0));
            pairs++;
        }

        return total / pairs;
    }

    private static double stackAt(ClimateLap lap, NormalNoise stack, int blockX, int blockZ) {
        return lap.noise(stack, DETAIL_SCALE, lap.x(blockX, blockZ, 0, 50000.0, true),
                lap.z(blockZ, 0, 50000.0, EQUATOR_OFFSET, true), blockZ, lap.xScale(0, 50000.0, true));
    }

    private static byte[] permutations() {
        byte[] permutations = new byte[256];
        for (int index = 0; index < permutations.length; index++) {
            permutations[index] = (byte) (index * 167 + 13);
        }

        return permutations;
    }
}
