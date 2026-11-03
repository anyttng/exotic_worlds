package com.exoticworlds.compat.ultraterraforged;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.exoticworlds.compat.terraforged.LapWave;
import com.exoticworlds.compat.terraforged.RtfLap;

import net.minecraft.core.Direction;

class DrumlinLatticeTest {
    private static final double LAP = 2048.0;

    private static final double NARROW_LAP = 1936.0;

    private static final double TINY_LAP = 512.0;

    private static final double SKEW = 272.0;

    private static final int DIRECTIONS = 36;

    private static final int POINTS = 400;

    private static final int SEED = 837836;

    private static final float SPAN = 3000.0F;

    private static final float CLOSE = 1.0E-3F;

    private static final float OFFSET_CLOSE = 1.0E-2F;

    private static final int NEIGHBORS = 1;

    private static final LapWave[] LAPS = {
        new LapWave(LAP, NARROW_LAP, RtfLap.NO_SKEW),
        new LapWave(LAP, LAP, SKEW),
        new LapWave(TINY_LAP, TINY_LAP, RtfLap.NO_SKEW),
        new LapWave(LAP, RtfLap.OPEN, RtfLap.NO_SKEW),
        new LapWave(RtfLap.OPEN, NARROW_LAP, RtfLap.NO_SKEW)
    };

    private record Seat(int column, int row, float alongOffset, float acrossOffset) {
    }

    @Test
    void aCellOnTheLapIsNeverLargerThanTheModsOwn() {
        for (LapWave lap : LAPS) {
            for (int i = 0; i < DIRECTIONS; i++) {
                double angle = 2.0 * Math.PI * i / DIRECTIONS;
                DrumlinLattice lattice = DrumlinLattice.over(lap, (float) Math.cos(angle), (float) Math.sin(angle));
                assertTrue(lattice.cellLength() > 0.0F && lattice.cellLength() <= DrumlinLattice.CELL_LENGTH + CLOSE,
                        "length " + lattice.cellLength() + " on " + lap + " at " + angle);
                assertTrue(lattice.rowWidth() > 0.0F && lattice.rowWidth() <= DrumlinLattice.CELL_WIDTH + CLOSE,
                        "width " + lattice.rowWidth() + " on " + lap + " at " + angle);
            }
        }
    }

    @Test
    void aCopyOneLapAwaySitsInTheSameFoldedCellsAtTheSameOffset() {
        Random random = new Random(SEED);
        for (LapWave lap : LAPS) {
            for (int i = 0; i < DIRECTIONS; i++) {
                double angle = 2.0 * Math.PI * i / DIRECTIONS;
                DrumlinLattice lattice = DrumlinLattice.over(lap, (float) Math.cos(angle), (float) Math.sin(angle));
                for (int point = 0; point < POINTS; point++) {
                    float x = (random.nextFloat() - 0.5F) * SPAN;
                    float z = (random.nextFloat() - 0.5F) * SPAN;
                    Seat here = seat(lattice, x, z);
                    if (onABorder(lattice, here)) {
                        continue;
                    }

                    String where = lap + " at " + angle + ", " + x + " " + z;
                    if (lap.loops(Direction.Axis.X)) {
                        assertSameCells(lattice, here, seat(lattice, x + (float) lap.xLap(), z), where);
                    }

                    if (lap.loops(Direction.Axis.Z)) {
                        assertSameCells(lattice, here, seat(lattice, x + (float) lap.skew(), z + (float) lap.zLap()),
                                where);
                    }
                }
            }
        }
    }

    private static Seat seat(DrumlinLattice lattice, float x, float z) {
        float along = lattice.along(x, z);
        float across = lattice.across(x, z);
        int row = (int) Math.floor(across / lattice.rowWidth());
        float shifted = along - row * lattice.rowShift();
        int column = (int) Math.floor(shifted / lattice.cellLength());
        return new Seat(column, row, shifted - column * lattice.cellLength(), across - row * lattice.rowWidth());
    }

    private static boolean onABorder(DrumlinLattice lattice, Seat seat) {
        return seat.alongOffset() < OFFSET_CLOSE || seat.alongOffset() > lattice.cellLength() - OFFSET_CLOSE
                || seat.acrossOffset() < OFFSET_CLOSE || seat.acrossOffset() > lattice.rowWidth() - OFFSET_CLOSE;
    }

    private static void assertSameCells(DrumlinLattice lattice, Seat here, Seat copy, String where) {
        assertEquals(here.alongOffset(), copy.alongOffset(), OFFSET_CLOSE, where);
        assertEquals(here.acrossOffset(), copy.acrossOffset(), OFFSET_CLOSE, where);
        for (int row = -NEIGHBORS; row <= NEIGHBORS; row++) {
            for (int column = -NEIGHBORS; column <= NEIGHBORS; column++) {
                int hereRow = here.row() + row;
                int copyRow = copy.row() + row;
                assertEquals(lattice.foldRow(hereRow), lattice.foldRow(copyRow), where);
                assertEquals(lattice.foldColumn(here.column() + column, hereRow),
                        lattice.foldColumn(copy.column() + column, copyRow), where);
            }
        }
    }
}
