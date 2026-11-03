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
                                assertEquals(lap.x(x, z, mode, distance),
                                        lap.x(x + lattice.x().domainLength, z, mode, distance));
                                if (lattice.z().loops()) {
                                    assertEquals(lap.x(x, z, mode, distance), lap.x(x + lattice.skew(),
                                            z + lattice.z().domainLength, mode, distance));
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
                        int step = (int) Math.ceil(lap.xScale(mode, distance));
                        for (int z : CROSS) {
                            int lower = lattice.x().lowerBound;
                            int upper = lower + lattice.x().domainLength;
                            assertTrue(Math.abs(lap.x(lower, z, mode, distance)
                                    - lap.x(upper - 1, z, mode, distance)) <= step);
                            assertTrue(Math.abs(lap.x(upper - 1, z, mode, distance)
                                    - lap.x(upper - 2, z, mode, distance)) <= step);
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
                        int alongZ = Math.abs(lap.z(z.lowerBound + span, mode, distance, EQUATOR_OFFSET)
                                - lap.z(z.lowerBound, mode, distance, EQUATOR_OFFSET));
                        int alongX = Math.abs(lap.x(fromX + span, atZ, mode, distance)
                                - lap.x(fromX, atZ, mode, distance));
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
                            assertEquals(lap.z(z, mode, distance, EQUATOR_OFFSET),
                                    lap.z(z + lattice.z().domainLength, mode, distance, EQUATOR_OFFSET));
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
                    int seam = Math.abs(lap.z(lower, 0, distance, EQUATOR_OFFSET)
                            - lap.z(upper - 1, 0, distance, EQUATOR_OFFSET));
                    int beside = Math.abs(lap.z(upper - 1, 0, distance, EQUATOR_OFFSET)
                            - lap.z(upper - 2, 0, distance, EQUATOR_OFFSET));
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
                        EQUATOR_OFFSET));
                assertEquals(Math.round(distance - EQUATOR_OFFSET), lap.z(0, 0, distance, EQUATOR_OFFSET));
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
                assertEquals(Math.round(Math.abs(z) * compression), lap.z(z, 3, 50000.0, EQUATOR_OFFSET));
                assertEquals(Math.round(Math.abs(z) * compression), lap.x(z, 0, RADIAL_MODE, 50000.0));
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
                    assertEquals(Math.round(coord * compression), cylinderX.z(coord, mode, 50000.0, EQUATOR_OFFSET));
                }

                assertEquals(Math.round(coord * compression), cylinderZ.x(coord, 0, RADIAL_MODE, 50000.0));
                for (int mode : MODES) {
                    assertEquals(Math.round(Math.abs(coord) * compression), cylinderX.x(coord, 0, mode, 50000.0));
                }
            }
        }
    }

    @Test
    void noEquatorialDistanceKeepsTheBlock() {
        ClimateLap lap = new ClimateLap(TORUS, 1.0);
        for (int z : CROSS) {
            assertEquals(z, lap.z(z, 0, 0.0, EQUATOR_OFFSET));
        }
    }
}
