package com.exoticworlds.compat.naturaltemperature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WrapDomain;

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

    private static final int[] MODES = {0, 1, 2, 3, 4, 5, 6, 9, 10};

    private static final int[] LATITUDE_MODES = {0, 1, 2, 5, 6};

    private static final int RADIAL_MODE = 3;

    private static final int EVEN_SPAN = 64;

    private static final int ROUNDING_SLACK = 2;

    private static final double[] EQUATORIAL_DISTANCES = {50000.0, 2048.0, 700.0, 100.0};

    private static final double EQUATOR_OFFSET = 1234.0;

    private static final int[] CROSS = {-3000, -1, 0, 517, 3999};

    private static final int[] OWN_SCALE_REACH = {1, 517, 3000, LAP / 2};

    private static final int WIDE_LAP_DISTANCE = 700;

    private static final int NARROW_LAP_DISTANCE = 50000;

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
                        for (int z : CROSS) {
                            int lower = lattice.x().lowerBound;
                            int upper = lower + lattice.x().domainLength;
                            assertTrue(Math.abs(lap.x(lower, z, mode, distance, true)
                                    - lap.x(upper - 1, z, mode, distance, true)) <= step);
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
                    assertEquals(Math.round(Math.abs(coord) * compression), cylinderX.x(coord, 0, mode, 50000.0, true));
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
}
