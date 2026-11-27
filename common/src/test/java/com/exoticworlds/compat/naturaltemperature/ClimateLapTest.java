package com.exoticworlds.compat.naturaltemperature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.fold.TranslationLattice;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.core.fold.WrapDomain;

import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.NoiseStack;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

class ClimateLapTest {
    private static final int LAP_BLOCKS = 8192;

    private static final int LAP_CHUNKS = LAP_BLOCKS / 16;

    private static final int SKEW_CHUNKS = 96;

    private static final WrapDomain LAP_DOMAIN = new WrapDomain(-LAP_BLOCKS / 2, LAP_BLOCKS / 2);

    private static final WrapDomain OFF_CENTER_DOMAIN = new WrapDomain(-1000, LAP_BLOCKS - 1000);

    private static final TranslationLattice TORUS = TranslationLattice.unskewed(LAP_DOMAIN, LAP_DOMAIN);

    private static final TranslationLattice SKEWED = WorldFolds.of(FlatShape.latticeTorus(
            ShapeFixtures.loopedBounds(-LAP_CHUNKS / 2, LAP_CHUNKS / 2, -LAP_CHUNKS / 2, LAP_CHUNKS / 2), SKEW_CHUNKS))
            .getBlockLattice();

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

    private static final int[] CUSTOM_MODES = {101, 102, 103, 104, 105};

    private static final int PAST_CUSTOM_MODES = 106;

    private static final String[] RADIAL_BODIES = {"circular", "circular6", "square6"};

    private static final String[] LATITUDE_BODIES = {"default", "simplified", "linear", "banded", "discrete",
            "another_mod_body"};

    private static final int[] NOISE_MODES = {0, RADIAL_MODE};

    private static final double SIDE_WAVE_RATE = 0.003;

    private static final int EVEN_SPAN_BLOCKS = 64;

    private static final int ROUNDING_SLACK = 2;

    private static final double[] EQUATORIAL_DISTANCES_BLOCKS = {50000.0, 2048.0, 700.0, 100.0};

    private static final double EQUATOR_OFFSET_BLOCKS = 1234.0;

    private static final int[] CROSS = {-3000, -1, 0, 517, 3999};

    private static final int[] OWN_SCALE_REACH_BLOCKS = {1, 517, 3000, LAP_BLOCKS / 2};

    private static final int WIDE_LAP_DISTANCE_BLOCKS = 700;

    private static final int NARROW_LAP_DISTANCE_BLOCKS = 50000;

    private static final boolean[] LOOPING_WORLD = {true, false};

    private static final int[] SIDE_REACH_BLOCKS = {1, 517, 3000};

    private static final double[] WAVE_RATES = {0.0005, 0.0007, 0.003, 0.00001};

    private static final double[] WAVE_LAPS = {LAP_BLOCKS, LAP_BLOCKS * 1.5, 512.0};

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

    private static final int NARROW_LAP_BLOCKS = 512;

    private static final int WIDE_LAP_BLOCKS = 65536;

    private static final int WIDE_STRIDE = 31;

    private static final double STEEPER_SLACK = 1.25;

    private static final double FLATTER_SLACK = 3.0;

    private static final double TURN_SLACK = 1.0E-6;

    private static final double WAVE = 37.5;

    private static final double DISTORTION = -11.25;

    private static final double FOLD_SLACK = 1.0E-6;

    private static final double[] BAND_WIDTHS_BLOCKS = {100.0, 500.0, 2500.0, 40000.0};

    private static final double NOISE_CELLS_PER_BLOCK = 0.046875;

    private static final double NOISE_SLACK = 1.0E-5;

    @Test
    void xTakesTheSameValueOneLapAwayOnEveryLattice() {
        for (TranslationLattice lattice : LATTICES) {
            if (!lattice.getX().isLooped()) {
                continue;
            }

            for (double compression : COMPRESSIONS) {
                ClimateLap lap = new ClimateLap(lattice, compression);
                for (int mode : MODES) {
                    for (double distanceBlocks : EQUATORIAL_DISTANCES_BLOCKS) {
                        for (int z : CROSS) {
                            for (int x = lattice.getX().lowerBound - 3; x < lattice.getX().lowerBound + 3; x++) {
                                assertEquals(lap.x(x, z, mode, distanceBlocks, true),
                                        lap.x(x + lattice.getX().domainLength, z, mode, distanceBlocks, true));
                                if (lattice.getZ().isLooped()) {
                                    assertEquals(lap.x(x, z, mode, distanceBlocks, true), lap.x(x + lattice.getSkew(),
                                            z + lattice.getZ().domainLength, mode, distanceBlocks, true));
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
            if (!lattice.getX().isLooped()) {
                continue;
            }

            for (double compression : COMPRESSIONS) {
                ClimateLap lap = new ClimateLap(lattice, compression);
                for (int mode : MODES) {
                    for (double distanceBlocks : EQUATORIAL_DISTANCES_BLOCKS) {
                        int step = (int) Math.ceil(lap.xScale(mode, distanceBlocks, true));
                        double turn = ClimateLap.isRadial(mode) ? 0.0
                                : lattice.getX().domainLength * lap.xScale(mode, distanceBlocks, true);
                        for (int z : CROSS) {
                            int lower = lattice.getX().lowerBound;
                            int upper = lower + lattice.getX().domainLength;
                            int jump = Math.abs(lap.x(lower, z, mode, distanceBlocks, true)
                                    - lap.x(upper - 1, z, mode, distanceBlocks, true));
                            assertTrue(jump <= step || Math.abs(jump - turn) <= step + 1, "jump " + jump);
                            assertTrue(Math.abs(lap.x(upper - 1, z, mode, distanceBlocks, true)
                                    - lap.x(upper - 2, z, mode, distanceBlocks, true)) <= step);
                        }
                    }
                }
            }
        }
    }

    @Test
    void xAdvancesAsTheLatitudeOnALoopedZ() {
        for (TranslationLattice lattice : LOOPED_Z) {
            WrapDomain x = lattice.getX();
            WrapDomain z = lattice.getZ();
            int fromX = x.isLooped() ? x.lowerBound + x.domainLength / 2 : 0;
            int atZ = z.lowerBound + z.domainLength / 2;
            for (double compression : COMPRESSIONS) {
                ClimateLap lap = new ClimateLap(lattice, compression);
                for (int mode : LATITUDE_MODES) {
                    for (double distanceBlocks : EQUATORIAL_DISTANCES_BLOCKS) {
                        int spanBlocks = Math.min(EVEN_SPAN_BLOCKS, z.domainLength / (2 * lap.bands(distanceBlocks)));
                        int alongZ = Math.abs(lap.z(z.lowerBound + spanBlocks, mode, distanceBlocks, EQUATOR_OFFSET_BLOCKS, true)
                                - lap.z(z.lowerBound, mode, distanceBlocks, EQUATOR_OFFSET_BLOCKS, true));
                        int alongX = Math.abs(lap.x(fromX + spanBlocks, atZ, mode, distanceBlocks, true)
                                - lap.x(fromX, atZ, mode, distanceBlocks, true));
                        assertTrue(Math.abs(alongZ - alongX) <= ROUNDING_SLACK,
                                "z " + alongZ + " x " + alongX + " over " + spanBlocks);
                    }
                }
            }
        }
    }

    @Test
    void zTakesTheSameValueOneLapAwayInEveryMode() {
        for (TranslationLattice lattice : LATTICES) {
            if (!lattice.getZ().isLooped()) {
                continue;
            }

            for (double compression : COMPRESSIONS) {
                ClimateLap lap = new ClimateLap(lattice, compression);
                for (int mode : MODES) {
                    for (double distanceBlocks : EQUATORIAL_DISTANCES_BLOCKS) {
                        for (int z = lattice.getZ().lowerBound - 3; z < lattice.getZ().lowerBound + 3; z++) {
                            assertEquals(lap.z(z, mode, distanceBlocks, EQUATOR_OFFSET_BLOCKS, true),
                                    lap.z(z + lattice.getZ().domainLength, mode, distanceBlocks, EQUATOR_OFFSET_BLOCKS, true));
                        }
                    }
                }
            }
        }
    }

    @Test
    void latitudeStepsAcrossTheSeamAsBesideIt() {
        for (TranslationLattice lattice : LATTICES) {
            if (!lattice.getZ().isLooped()) {
                continue;
            }

            for (double compression : COMPRESSIONS) {
                ClimateLap lap = new ClimateLap(lattice, compression);
                for (double distanceBlocks : EQUATORIAL_DISTANCES_BLOCKS) {
                    int lower = lattice.getZ().lowerBound;
                    int upper = lower + lattice.getZ().domainLength;
                    int seam = Math.abs(lap.z(lower, 0, distanceBlocks, EQUATOR_OFFSET_BLOCKS, true)
                            - lap.z(upper - 1, 0, distanceBlocks, EQUATOR_OFFSET_BLOCKS, true));
                    int beside = Math.abs(lap.z(upper - 1, 0, distanceBlocks, EQUATOR_OFFSET_BLOCKS, true)
                            - lap.z(upper - 2, 0, distanceBlocks, EQUATOR_OFFSET_BLOCKS, true));
                    assertTrue(seam <= beside + 1, "seam " + seam + " beside " + beside);
                }
            }
        }
    }

    @Test
    void latitudeHasAPoleAtTheSeamAndTheEquatorAtTheCenter() {
        for (double compression : COMPRESSIONS) {
            ClimateLap lap = new ClimateLap(TORUS, compression);
            for (double distanceBlocks : EQUATORIAL_DISTANCES_BLOCKS) {
                assertEquals(Math.round(-distanceBlocks - EQUATOR_OFFSET_BLOCKS), lap.z(LAP_DOMAIN.lowerBound, 0, distanceBlocks,
                        EQUATOR_OFFSET_BLOCKS, true));
                assertEquals(Math.round(distanceBlocks - EQUATOR_OFFSET_BLOCKS), lap.z(0, 0, distanceBlocks, EQUATOR_OFFSET_BLOCKS, true));
            }
        }
    }

    @Test
    void bandsAreTheFewestOddCountNoWiderThanTheCompressedSpan() {
        for (double compression : COMPRESSIONS) {
            ClimateLap lap = new ClimateLap(TORUS, compression);
            for (double distanceBlocks : EQUATORIAL_DISTANCES_BLOCKS) {
                int bands = lap.bands(distanceBlocks);
                double capped = 2.0 * distanceBlocks / compression;
                assertEquals(1, bands % 2);
                assertTrue(LAP_BLOCKS / (2.0 * bands) <= capped, bands + " bands, cap " + capped);
                assertTrue(bands == 1 || LAP_BLOCKS / (2.0 * (bands - 2)) > capped, bands + " bands, cap " + capped);
            }
        }
    }

    @Test
    void radialModesKeepTheDistanceFromTheLapCenter() {
        for (double compression : COMPRESSIONS) {
            ClimateLap lap = new ClimateLap(TORUS, compression);
            for (int z : CROSS) {
                assertEquals(Math.round(Math.abs(z) * compression), lap.z(z, 3, 50000.0, EQUATOR_OFFSET_BLOCKS, true));
                assertEquals(Math.round(Math.abs(z) * compression), lap.x(z, 0, RADIAL_MODE, 50000.0, true));
            }
        }
    }

    @Test
    void radialModesKeepTheSideOfTheLapCenterForTheDisplacement() {
        for (TranslationLattice lattice : List.of(TORUS, SKEWED, CYLINDER_X, OFF_CENTER)) {
            int center = lattice.getX().lowerBound + lattice.getX().domainLength / 2;
            for (double compression : COMPRESSIONS) {
                ClimateLap lap = new ClimateLap(lattice, compression);
                for (int mode : RADIAL_MODES) {
                    assertSignedX(lap, lattice.getX(), center, mode);
                }
            }
        }
    }

    private static void assertSignedX(ClimateLap lap, WrapDomain x, int center, int mode) {
        double handedLap = x.domainLength * lap.xScale(mode, 50000.0, true);
        int differing = 0;
        for (int fromCenter : SIDE_REACH_BLOCKS) {
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
                    assertEquals(Math.round(coord * compression), cylinderX.z(coord, mode, 50000.0, EQUATOR_OFFSET_BLOCKS, true));
                }

                assertEquals(Math.round(coord * compression), cylinderZ.x(coord, 0, RADIAL_MODE, 50000.0, true));
                for (int mode : MODES) {
                    assertEquals(Math.round((ClimateLap.isRadial(mode) ? Math.abs(coord) : coord) * compression),
                            cylinderX.x(coord, 0, mode, 50000.0, true));
                }
            }
        }
    }

    @Test
    void noEquatorialDistanceKeepsTheBlock() {
        ClimateLap lap = new ClimateLap(TORUS, 1.0);
        for (int z : CROSS) {
            assertEquals(z, lap.z(z, 0, 0.0, EQUATOR_OFFSET_BLOCKS, true));
        }
    }

    @Test
    void ownScaleKeepsTheMirroredDistanceFromTheLapCenter() {
        for (TranslationLattice lattice : LOOPED_Z) {
            for (double compression : COMPRESSIONS) {
                assertOwnScaleZ(new ClimateLap(lattice, compression), lattice.getZ(), compression);
            }
        }
    }

    private static void assertOwnScaleZ(ClimateLap lap, WrapDomain z, double compression) {
        int center = z.lowerBound + z.domainLength / 2;
        for (int mode : LATITUDE_MODES) {
            for (double distanceBlocks : EQUATORIAL_DISTANCES_BLOCKS) {
                assertEquals(0, lap.z(center, mode, distanceBlocks, EQUATOR_OFFSET_BLOCKS, false));
                for (int fromCenter : OWN_SCALE_REACH_BLOCKS) {
                    int expected = (int) -Math.round(fromCenter * compression);
                    assertEquals(expected, lap.z(center + fromCenter, mode, distanceBlocks, EQUATOR_OFFSET_BLOCKS, false));
                    assertEquals(expected, lap.z(center - fromCenter, mode, distanceBlocks, EQUATOR_OFFSET_BLOCKS, false));
                    assertEquals(expected,
                            lap.z(center + fromCenter + z.domainLength, mode, distanceBlocks, EQUATOR_OFFSET_BLOCKS, false));
                }
            }
        }
    }

    @Test
    void ownScaleReachesTheModsPoleOnlyOnALapWiderThanTheBand() {
        ClimateLap lap = new ClimateLap(TORUS, 1.0);
        int edge = LAP_DOMAIN.lowerBound;
        assertEquals(-WIDE_LAP_DISTANCE_BLOCKS, lap.z(WIDE_LAP_DISTANCE_BLOCKS, 0, WIDE_LAP_DISTANCE_BLOCKS, 0.0, false));
        assertEquals(-WIDE_LAP_DISTANCE_BLOCKS, lap.z(-WIDE_LAP_DISTANCE_BLOCKS, 0, WIDE_LAP_DISTANCE_BLOCKS, 0.0, false));
        assertEquals(-2 * WIDE_LAP_DISTANCE_BLOCKS, lap.z(2 * WIDE_LAP_DISTANCE_BLOCKS, 0, WIDE_LAP_DISTANCE_BLOCKS, 0.0, false));
        assertEquals(-LAP_BLOCKS / 2, lap.z(edge, 0, WIDE_LAP_DISTANCE_BLOCKS, 0.0, false));
        assertEquals(-LAP_BLOCKS / 2, lap.z(edge, 0, NARROW_LAP_DISTANCE_BLOCKS, 0.0, false));
        assertTrue(lap.z(edge, 0, WIDE_LAP_DISTANCE_BLOCKS, 0.0, false) <= -WIDE_LAP_DISTANCE_BLOCKS);
        assertTrue(lap.z(edge, 0, NARROW_LAP_DISTANCE_BLOCKS, 0.0, false) > -NARROW_LAP_DISTANCE_BLOCKS);
    }

    @Test
    void ownScaleStepsAcrossTheZSeamAsBesideIt() {
        for (TranslationLattice lattice : LOOPED_Z) {
            int lower = lattice.getZ().lowerBound;
            int upper = lower + lattice.getZ().domainLength;
            for (double compression : COMPRESSIONS) {
                ClimateLap lap = new ClimateLap(lattice, compression);
                int seam = Math.abs(lap.z(lower, 0, 50000.0, EQUATOR_OFFSET_BLOCKS, false)
                        - lap.z(upper - 1, 0, 50000.0, EQUATOR_OFFSET_BLOCKS, false));
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
        WrapDomain x = lattice.getX();
        WrapDomain z = lattice.getZ();
        int fromX = x.isLooped() ? x.lowerBound + x.domainLength / 2 : 0;
        int atZ = z.lowerBound + z.domainLength / 2;
        long expected = Math.round(EVEN_SPAN_BLOCKS * compression);
        for (int mode : LATITUDE_MODES) {
            for (double distanceBlocks : EQUATORIAL_DISTANCES_BLOCKS) {
                assertEquals(compression, lap.xScale(mode, distanceBlocks, false));
                int alongZ = Math.abs(lap.z(atZ + EVEN_SPAN_BLOCKS, mode, distanceBlocks, EQUATOR_OFFSET_BLOCKS, false)
                        - lap.z(atZ, mode, distanceBlocks, EQUATOR_OFFSET_BLOCKS, false));
                int alongX = Math.abs(lap.x(fromX + EVEN_SPAN_BLOCKS, atZ, mode, distanceBlocks, false)
                        - lap.x(fromX, atZ, mode, distanceBlocks, false));
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
        int lower = lattice.getX().lowerBound;
        for (int z : CROSS) {
            for (int x = lower - 3; x < lower + 3; x++) {
                int here = lap.x(x, z, 0, 50000.0, false);
                assertEquals(here, lap.x(x + lattice.getX().domainLength, z, 0, 50000.0, false));
                assertEquals(here, lap.x(x + lattice.getSkew(), z + lattice.getZ().domainLength, 0, 50000.0, false));
            }
        }
    }

    @Test
    void xKeepsItsSideOfTheLapCenterOutsideRadialModes() {
        for (TranslationLattice lattice : List.of(TORUS, CYLINDER_X, OFF_CENTER)) {
            int center = lattice.getX().lowerBound + lattice.getX().domainLength / 2;
            for (double compression : COMPRESSIONS) {
                ClimateLap lap = new ClimateLap(lattice, compression);
                for (int mode : LATITUDE_MODES) {
                    for (boolean looping : LOOPING_WORLD) {
                        for (int fromCenter : SIDE_REACH_BLOCKS) {
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
        assertEquals(0.0, ClimateLap.wave(CELLS, WAVE_SEED, 517.0, LAP_BLOCKS, 0.0, 0.0005));
        assertEquals(0.0, ClimateLap.wave(CELLS, WAVE_SEED, 517.0, LAP_BLOCKS, WAVE_AMPLITUDE, 0.0));
    }

    @Test
    void theSixZoneCirclesAreRadialAndTheDiscreteModesFollowTheLatitude() {
        for (int mode : RADIAL_MODES) {
            assertTrue(ClimateLap.isRadial(mode), "mode " + mode);
        }

        for (int mode : LATITUDE_MODES) {
            assertTrue(!ClimateLap.isRadial(mode), "mode " + mode);
        }
    }

    @Test
    void aCustomModeNamingARadialBodyIsRadial() {
        for (int mode : CUSTOM_MODES) {
            for (String body : RADIAL_BODIES) {
                assertTrue(ClimateLap.isRadial(ClimateLap.mode(mode, body)), "mode " + mode + " with " + body);
            }

            for (String body : LATITUDE_BODIES) {
                assertTrue(!ClimateLap.isRadial(ClimateLap.mode(mode, body)), "mode " + mode + " with " + body);
            }

            assertTrue(!ClimateLap.isRadial(ClimateLap.mode(mode, null)), "mode " + mode + " with no id");
        }
    }

    @Test
    void aModeOutsideTheCustomOnesKeepsItsNumberWhateverTheIdNames() {
        for (int mode : MODES) {
            assertEquals(mode, ClimateLap.mode(mode, RADIAL_BODIES[0]));
        }

        assertEquals(PAST_CUSTOM_MODES, ClimateLap.mode(PAST_CUSTOM_MODES, RADIAL_BODIES[0]));
    }

    @Test
    void theLatitudeFoldsAfterTheDisplacement() {
        for (TranslationLattice lattice : LOOPED_Z) {
            for (double compression : COMPRESSIONS) {
                ClimateLap lap = new ClimateLap(lattice, compression);
                for (double distanceBlocks : EQUATORIAL_DISTANCES_BLOCKS) {
                    for (boolean looping : LOOPING_WORLD) {
                        assertFoldAfterDisplacement(lap, lattice.getZ(), distanceBlocks, looping);
                    }
                }
            }
        }
    }

    private static void assertFoldAfterDisplacement(ClimateLap lap, WrapDomain z, double distanceBlocks, boolean looping) {
        double scale = lap.xScale(0, distanceBlocks, looping);
        for (int blockZ : CROSS) {
            int handed = lap.z(blockZ, 0, distanceBlocks, EQUATOR_OFFSET_BLOCKS, looping);
            double carried = lap.carriedWave(blockZ, handed, WAVE, DISTORTION, 0, distanceBlocks, EQUATOR_OFFSET_BLOCKS, looping);
            double displaced = lap.latitude(blockZ + (DISTORTION + WAVE) / scale, distanceBlocks, EQUATOR_OFFSET_BLOCKS, looping);
            assertEquals(displaced, handed + DISTORTION + carried, FOLD_SLACK);
            assertEquals(carried, lap.carriedWave(blockZ + z.domainLength, handed, WAVE, DISTORTION, 0, distanceBlocks,
                    EQUATOR_OFFSET_BLOCKS, looping), FOLD_SLACK);
        }
    }

    @Test
    void aDisplacementMovesBothSidesOfTheCenterTheSameWay() {
        ClimateLap lap = new ClimateLap(TORUS, 1.0);
        for (boolean looping : LOOPING_WORLD) {
            double north = lap.latitude(-EVEN_SPAN_BLOCKS + WAVE, 2048.0, 0.0, looping)
                    - lap.latitude(-EVEN_SPAN_BLOCKS, 2048.0, 0.0, looping);
            double south = lap.latitude(EVEN_SPAN_BLOCKS + WAVE, 2048.0, 0.0, looping)
                    - lap.latitude(EVEN_SPAN_BLOCKS, 2048.0, 0.0, looping);
            assertEquals(-north, south, FOLD_SLACK);
            assertTrue(Math.abs(north) > FOLD_SLACK);
        }
    }

    @Test
    void theWaveIsLeftAloneOnAnOpenZAndInRadialModes() {
        assertEquals(WAVE, new ClimateLap(CYLINDER_X, 2.0).carriedWave(517, 1034, WAVE, DISTORTION, 0, 50000.0,
                EQUATOR_OFFSET_BLOCKS, true));
        assertEquals(WAVE, new ClimateLap(TORUS, 2.0).carriedWave(517, 1034, WAVE, DISTORTION, RADIAL_MODE, 50000.0,
                EQUATOR_OFFSET_BLOCKS, true));
    }

    @Test
    void humidityBandsRepeatWholeTimesOnTheXLapAndNeverWider() {
        for (TranslationLattice lattice : List.of(TORUS, SKEWED, CYLINDER_X, OFF_CENTER)) {
            for (double compression : COMPRESSIONS) {
                ClimateLap lap = new ClimateLap(lattice, compression);
                for (int mode : MODES) {
                    assertBandsOnTheLap(lap, lattice.getX(), mode);
                }
            }
        }

        assertEquals(500.0, new ClimateLap(CYLINDER_Z, 2.0).lapBandWidthBlocks(500.0, 0, 50000.0, true));
    }

    private static void assertBandsOnTheLap(ClimateLap lap, WrapDomain x, int mode) {
        double handedLap = x.domainLength * lap.xScale(mode, 50000.0, true);
        for (double widthBlocks : BAND_WIDTHS_BLOCKS) {
            double fitted = lap.lapBandWidthBlocks(widthBlocks, mode, 50000.0, true);
            double repeats = handedLap / (4.0 * fitted);
            assertEquals(Math.rint(repeats), repeats, TURN_SLACK);
            assertTrue(fitted <= widthBlocks + TURN_SLACK, fitted + " over " + widthBlocks);
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
        int lapX = lattice.getX().isLooped() ? lattice.getX().domainLength : 0;
        int lapZ = lattice.getZ().isLooped() ? lattice.getZ().domainLength : 0;
        int differing = 0;
        for (int x : CROSS) {
            for (int z : CROSS) {
                double here = noiseAt(lap, permutations, x, z, mode);
                assertEquals(here, noiseAt(lap, permutations, x + lapX, z, mode), NOISE_SLACK);
                assertEquals(here, noiseAt(lap, permutations, x + lattice.getSkew(), z + lapZ, mode), NOISE_SLACK);
                int mirrorX = 2 * (lattice.getX().lowerBound + lapX / 2) - x;
                int mirrorZ = 2 * (lattice.getZ().lowerBound + lapZ / 2) - z;
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
                lap.signedX(blockX, blockZ, mode, 50000.0, true), lap.z(blockZ, mode, 50000.0, EQUATOR_OFFSET_BLOCKS, true),
                blockZ, scale, lap.shrink(NOISE_CELLS_PER_BLOCK, scale));
    }

    @Test
    void aNoiseStackRepeatsOnTheLapLayerByLayer() {
        NoiseStack stack = (NoiseStack) NormalNoise.createParity(-7, 1.0, 1.0)
                .create(new LegacyRandomSource(NOISE_SEED));
        for (TranslationLattice lattice : LATTICES) {
            ClimateLap lap = new ClimateLap(lattice, 1.5);
            int lapX = lattice.getX().isLooped() ? lattice.getX().domainLength : 0;
            int lapZ = lattice.getZ().isLooped() ? lattice.getZ().domainLength : 0;
            double span = 0.0;
            for (int x : CROSS) {
                for (int z : CROSS) {
                    double here = stackAt(lap, stack, x, z);
                    assertEquals(here, stackAt(lap, stack, x + lapX, z), NOISE_SLACK);
                    assertEquals(here, stackAt(lap, stack, x + lattice.getSkew(), z + lapZ), NOISE_SLACK);
                    span = Math.max(span, Math.abs(here));
                }
            }

            assertTrue(span > NOISE_SLACK, "span " + span);
        }
    }

    @Test
    void aNoiseStackRepeatsOneHandedLapAlongX() {
        NoiseStack large = (NoiseStack) NormalNoise.createParity(-14, -14.0, -14.0, -14.0)
                .create(new LegacyRandomSource(NOISE_SEED));
        NoiseStack detail = (NoiseStack) NormalNoise.createParity(-7, 1.0).create(new LegacyRandomSource(NOISE_SEED));
        for (TranslationLattice lattice : List.of(TORUS, SKEWED, CYLINDER_X, OFF_CENTER)) {
            for (double distanceBlocks : EQUATORIAL_DISTANCES_BLOCKS) {
                ClimateLap lap = new ClimateLap(lattice, 1.5);
                double scale = lap.xScale(0, distanceBlocks, true);
                double handedLap = lattice.getX().domainLength * scale;
                for (int z : CROSS) {
                    for (NoiseStack stack : List.of(large, detail)) {
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
        NoiseStack large = (NoiseStack) NormalNoise.createParity(-14, -14.0, -14.0, -14.0)
                .create(new LegacyRandomSource(NOISE_SEED));
        WrapDomain narrow = new WrapDomain(-NARROW_LAP_BLOCKS / 2, NARROW_LAP_BLOCKS / 2);
        WrapDomain wide = new WrapDomain(-WIDE_LAP_BLOCKS / 2, WIDE_LAP_BLOCKS / 2);
        double squeezed = meanSlope(new ClimateLap(TranslationLattice.unskewed(narrow, narrow), 1.0), large,
                NARROW_LAP_BLOCKS, 1);
        double shipped = meanSlope(new ClimateLap(TranslationLattice.unskewed(wide, wide), 1.0), large, WIDE_LAP_BLOCKS,
                WIDE_STRIDE);
        assertTrue(squeezed < STEEPER_SLACK * shipped && shipped < FLATTER_SLACK * squeezed,
                "squeezed " + squeezed + " shipped " + shipped);
    }

    private static double meanSlope(ClimateLap lap, NoiseStack stack, int lapBlocks, int stride) {
        double total = 0.0;
        int pairs = 0;
        for (int z = -lapBlocks / 2; z < lapBlocks / 2 - 1; z += stride) {
            total += Math.abs(lap.noise(stack, DETAIL_SCALE, 0.0, 0.0, z + 1, 1.0)
                    - lap.noise(stack, DETAIL_SCALE, 0.0, 0.0, z, 1.0));
            pairs++;
        }

        return total / pairs;
    }

    private static double stackAt(ClimateLap lap, NoiseStack stack, int blockX, int blockZ) {
        return lap.noise(stack, DETAIL_SCALE, lap.x(blockX, blockZ, 0, 50000.0, true),
                lap.z(blockZ, 0, 50000.0, EQUATOR_OFFSET_BLOCKS, true), blockZ, lap.xScale(0, 50000.0, true));
    }

    private static byte[] permutations() {
        byte[] permutations = new byte[256];
        for (int index = 0; index < permutations.length; index++) {
            permutations[index] = (byte) (index * 167 + 13);
        }

        return permutations;
    }
}
