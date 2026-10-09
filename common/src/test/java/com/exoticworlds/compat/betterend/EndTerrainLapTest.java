package com.exoticworlds.compat.betterend;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.exoticworlds.compat.wover.OpenSimplexStandIn;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;

import net.minecraft.core.Direction;

class EndTerrainLapTest {
    private static final long SEED = 0x1040L;

    private static final WorldLoopBounds BOUNDS = new WorldLoopBounds(-40, 40, -24, 24);

    private static final int SKEW_CHUNKS = 13;

    private static final int CYLINDER_CHUNKS = 80;

    private static final int STEP = 37;

    private static final double[] FREQUENCIES = {0.01, 0.05, 0.1};

    private static final int SURFACE_Y = 64;

    private static final double TOLERANCE = 1.0E-9;

    @Test
    void onASkewedEndTheFoldReadsTheSameAlongBothLatticeVectors() {
        EndTerrainLap lap = skewed();
        TranslationLattice lattice = lattice();
        int widthX = lattice.x().domainLength;
        int widthZ = lattice.z().domainLength;
        for (int x = 3 * lattice.x().lowerBound; x < 3 * lattice.x().upperBound; x += STEP) {
            for (int z = 3 * lattice.z().lowerBound; z < 3 * lattice.z().upperBound; z += STEP) {
                String where = x + ", " + z;
                assertEquals(lap.foldX(x, z), lap.foldX(x + widthX, z), where + " one X lap on");
                assertEquals(lap.foldX(x, z), lap.foldX(x + lattice.skew(), z + widthZ), where + " one Z lap on");
                assertEquals(lap.foldZ(z), lap.foldZ(z + widthZ), where + " one Z lap on");
            }
        }
    }

    @Test
    void onASkewedEndTheFrameTakesAZLapAsNoShiftAlongX() {
        EndTerrainLap lap = skewed();
        TranslationLattice lattice = lattice();
        for (int x = lattice.x().lowerBound; x < lattice.x().upperBound; x += STEP) {
            for (int z = lattice.z().lowerBound; z < lattice.z().upperBound; z += STEP) {
                String where = x + ", " + z;
                assertEquals(lap.frameX(x, z) + lattice.x().domainLength,
                        lap.frameX(x + lattice.x().domainLength, z), TOLERANCE, where + " one X lap on");
                assertEquals(lap.frameX(x, z), lap.frameX(x + lattice.skew(), z + lattice.z().domainLength),
                        TOLERANCE, where + " one Z lap on");
            }
        }
    }

    @Test
    void onASkewedEndTheWarpAndDetailNoiseReadTheSameAlongBothLatticeVectors() {
        EndTerrainLap lap = skewed();
        TranslationLattice lattice = lattice();
        int widthX = lattice.x().domainLength;
        int widthZ = lattice.z().domainLength;
        for (double frequency : FREQUENCIES) {
            for (int x = lattice.x().lowerBound; x < lattice.x().upperBound; x += STEP) {
                for (int z = lattice.z().lowerBound; z < lattice.z().upperBound; z += STEP) {
                    String where = x + ", " + z + " at " + frequency;
                    assertEquals(warp(lap, x, z, frequency), warp(lap, x + widthX, z, frequency), TOLERANCE,
                            where + " one X lap on");
                    assertEquals(warp(lap, x, z, frequency), warp(lap, x + lattice.skew(), z + widthZ, frequency),
                            TOLERANCE, where + " one Z lap on");
                    assertEquals(detail(lap, x, z, frequency),
                            detail(lap, x + lattice.skew(), z + widthZ, frequency), TOLERANCE,
                            where + " one Z lap on in three dimensions");
                }
            }
        }
    }

    @Test
    void anUnskewedEndKeepsTheFrameAndTheFoldPerAxis() {
        EndTerrainLap lap = new EndTerrainLap(WorldFolds.of(FlatShape.torus(BOUNDS)), SEED);
        TranslationLattice lattice = WorldFolds.of(FlatShape.torus(BOUNDS)).blockLattice();
        for (int x = 3 * lattice.x().lowerBound; x < 3 * lattice.x().upperBound; x += STEP) {
            for (int z = 3 * lattice.z().lowerBound; z < 3 * lattice.z().upperBound; z += STEP) {
                assertEquals(x, lap.frameX(x, z), x + ", " + z);
                assertEquals(lattice.x().wrap(x), lap.foldX(x, z), x + ", " + z);
                assertEquals(lattice.z().wrap(z), lap.foldZ(z), x + ", " + z);
            }
        }
    }

    @Test
    void aCylinderEndLeavesTheOpenAxisUnbounded() {
        EndTerrainLap lap = new EndTerrainLap(
                WorldFolds.of(FlatShape.cylinder(WorldLoopBounds.ofWidth(Direction.Axis.X, CYLINDER_CHUNKS))), SEED);
        assertEquals(OpenSimplexStandIn.UNBOUNDED, lap.period(Direction.Axis.Z, 1.0));
    }

    private static double warp(EndTerrainLap lap, int x, int z, double frequency) {
        return lap.first().eval(lap.frameX(x, z) * frequency, z * frequency,
                lap.period(Direction.Axis.X, 1.0) * frequency, lap.period(Direction.Axis.Z, 1.0) * frequency);
    }

    private static double detail(EndTerrainLap lap, int x, int z, double frequency) {
        return lap.second().eval(lap.frameX(x, z) * frequency, SURFACE_Y * frequency, z * frequency,
                lap.period(Direction.Axis.X, 1.0) * frequency, lap.period(Direction.Axis.Z, 1.0) * frequency);
    }

    private static EndTerrainLap skewed() {
        return new EndTerrainLap(WorldFolds.of(FlatShape.latticeTorus(BOUNDS, SKEW_CHUNKS)), SEED);
    }

    private static TranslationLattice lattice() {
        return WorldFolds.of(FlatShape.latticeTorus(BOUNDS, SKEW_CHUNKS)).blockLattice();
    }
}
