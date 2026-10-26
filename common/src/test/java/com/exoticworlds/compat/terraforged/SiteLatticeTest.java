package com.exoticworlds.compat.terraforged;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.core.Direction;

class SiteLatticeTest {
    private static final double LAP = 2048.0;

    private static final double NARROW_LAP = 1936.0;

    private static final double SKEW = 272.0;

    private static final float[] SPACINGS = {20.0F, 24.0F, 44.0F, 96.0F, 176.0F, 520.0F, 1400.0F, 3600.0F};

    private static final double CLOSE = 1.0E-3;

    private static final float STEP = 0.25F;

    @Test
    void aCellOnTheLapIsNeverLargerThanTheModsOwnAndTilesTheLap() {
        for (float spacing : SPACINGS) {
            SiteLattice lattice = SiteLattice.over(LAP, NARROW_LAP, RtfLap.NO_SKEW, spacing);
            for (Direction.Axis axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
                float snapped = lattice.spacing(axis);
                assertTrue(snapped <= spacing, spacing + " grew to " + snapped + " on " + axis);
                int cells = axis == Direction.Axis.X ? lattice.xCells() : lattice.zCells();
                double lap = axis == Direction.Axis.X ? LAP : NARROW_LAP;
                assertEquals(lap, cells * (double) snapped, CLOSE, spacing + " on " + axis);
                assertTrue(cells >= 2, "a torus keeps at least two cells per lap");
            }
        }
    }

    @Test
    void anOpenAxisKeepsTheModsOwnSpacing() {
        SiteLattice lattice = SiteLattice.over(LAP, RtfLap.OPEN, RtfLap.NO_SKEW, SPACINGS[6]);
        assertEquals(SPACINGS[6], lattice.zSpacing());
        assertEquals(RtfLap.NO_PERIOD, lattice.zCells());
        assertEquals(1, SiteLattice.over(LAP, RtfLap.OPEN, RtfLap.NO_SKEW, SPACINGS[7]).xCells());
    }

    @Test
    void aWindowCellPastTheXSeamIsTheLastCellWithThePointOneLapOn() {
        SiteLattice lattice = SiteLattice.over(LAP, LAP, RtfLap.NO_SKEW, SPACINGS[5]);
        SiteLattice.Seat seat = lattice.seat(-1, 2, 0, 2, 5.0F, 700.0F);
        assertEquals(lattice.xCells() - 1, seat.cellX());
        assertEquals(2, seat.cellZ());
        assertEquals(5.0 + LAP, seat.x(), CLOSE);
        assertEquals(700.0, seat.z(), CLOSE);
        SiteLattice.Seat far = lattice.seat(lattice.xCells(), 0, lattice.xCells() - 1, 0, (float) LAP - 3.0F, 1.0F);
        assertEquals(0, far.cellX());
        assertEquals(-3.0, far.x(), CLOSE);
    }

    @Test
    void aWindowRowPastTheZSeamIsTheLastRowAroundThePointMovedByTheSkew() {
        SiteLattice lattice = SiteLattice.over(LAP, LAP, SKEW, SPACINGS[2]);
        float x = 1000.0F;
        float z = 3.0F;
        int pointX = lattice.cellX(x);
        SiteLattice.Seat seat = lattice.seat(pointX + 1, -1, pointX, 0, x, z);
        assertEquals(lattice.zCells() - 1, seat.cellZ());
        assertEquals(x + SKEW, seat.x(), CLOSE);
        assertEquals(z + LAP, seat.z(), CLOSE);
        assertEquals(lattice.cellX((float) (x + SKEW)) + 1, seat.cellX());
    }

    @Test
    void aCopyCellOneXLapOnHashesTheCanonicalCellAndSitsOneLapOn() {
        SiteLattice lattice = SiteLattice.over(LAP, NARROW_LAP, RtfLap.NO_SKEW, SPACINGS[4]);
        for (int cell = -2; cell <= 2; cell++) {
            int copy = cell + lattice.xCells();
            assertEquals(lattice.canonicalX(cell), lattice.canonicalX(copy));
            assertEquals(cell * (double) lattice.xSpacing() + LAP, copy * (double) lattice.xSpacing(), CLOSE);
            assertEquals(cell, lattice.copyX(cell, 0, 0, 5.0F), "a canonical row is its own copy");
        }
    }

    @Test
    void aCopyCellOneZLapOnHashesTheCanonicalCellAndSitsOneSkewedLapOn() {
        SiteLattice lattice = SiteLattice.over(LAP, LAP, SKEW, SPACINGS[2]);
        for (int row = -2; row <= 2; row++) {
            int copy = row + lattice.zCells();
            assertEquals(lattice.canonicalZ(row), lattice.canonicalZ(copy));
            assertEquals(SKEW, lattice.copySkew(copy) - lattice.copySkew(row), CLOSE);
            assertEquals(row * (double) lattice.zSpacing() + LAP, copy * (double) lattice.zSpacing(), CLOSE);
        }
    }

    @Test
    void aChunkAndItsCopyOneZLapOnNameTheSameCopyCell() {
        SiteLattice lattice = SiteLattice.over(LAP, LAP, SKEW, SPACINGS[2]);
        float x = 1000.0F;
        int pointX = lattice.cellX(x);
        int movedPointX = lattice.cellX((float) (x + SKEW));
        for (int cell = pointX - 1; cell <= pointX + 1; cell++) {
            int copy = lattice.copyX(cell, -1, pointX, x);
            assertEquals(cell + movedPointX - pointX, copy, "cell " + cell);
            assertEquals(lattice.seat(cell, -1, pointX, 0, x, 3.0F).cellX(), lattice.canonicalX(copy));
        }
    }

    @Test
    void anInterpolatedGridJoinsAcrossTheXSeam() {
        SiteLattice lattice = SiteLattice.over(LAP, NARROW_LAP, RtfLap.NO_SKEW, SPACINGS[3]);
        SiteLattice.Corner corner = SiteLatticeTest::corner;
        for (float z = 0.0F; z < 200.0F; z += 17.0F) {
            assertEquals(lattice.interpolate(0.0F, z, corner), lattice.interpolate((float) LAP, z, corner), CLOSE);
            assertEquals(lattice.interpolate(-STEP, z, corner), lattice.interpolate((float) LAP - STEP, z, corner),
                    CLOSE, "the point before the seam at z " + z);
        }
    }

    @Test
    void anInterpolatedGridJoinsAcrossASkewedZSeam() {
        SiteLattice lattice = SiteLattice.over(LAP, LAP, SKEW, SPACINGS[3]);
        SiteLattice.Corner corner = SiteLatticeTest::corner;
        for (float x = 0.0F; x < 400.0F; x += 23.0F) {
            assertEquals(lattice.interpolate((float) (x - SKEW), 0.0F, corner),
                    lattice.interpolate(x, (float) LAP, corner), CLOSE, "the row past the seam at x " + x);
        }
    }

    private static float corner(int cellX, int cellZ) {
        return (float) Math.sin(cellX * 12.9898 + cellZ * 78.233);
    }
}
