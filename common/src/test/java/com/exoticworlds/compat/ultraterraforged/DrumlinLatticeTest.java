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

    private static final LapWave[] LAPS = {
        new LapWave(LAP, NARROW_LAP, RtfLap.NO_SKEW),
        new LapWave(LAP, LAP, SKEW),
        new LapWave(TINY_LAP, TINY_LAP, RtfLap.NO_SKEW),
        new LapWave(LAP, RtfLap.OPEN, RtfLap.NO_SKEW),
        new LapWave(RtfLap.OPEN, NARROW_LAP, RtfLap.NO_SKEW)
    };

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
    void aDrumlinFieldRepeatsAlongEveryLoopingLapVector() {
        Random random = new Random(SEED);
        for (LapWave lap : LAPS) {
            for (int i = 0; i < DIRECTIONS; i++) {
                double angle = 2.0 * Math.PI * i / DIRECTIONS;
                DrumlinLattice lattice = DrumlinLattice.over(lap, (float) Math.cos(angle), (float) Math.sin(angle));
                for (int point = 0; point < POINTS; point++) {
                    float x = (random.nextFloat() - 0.5F) * SPAN;
                    float z = (random.nextFloat() - 0.5F) * SPAN;
                    float here = lattice.drumlin(SEED, x, z);
                    if (lap.loops(Direction.Axis.X)) {
                        assertEquals(here, lattice.drumlin(SEED, x + (float) lap.xLap(), z), CLOSE,
                                lap + " at " + angle + ", " + x + " " + z);
                    }

                    if (lap.loops(Direction.Axis.Z)) {
                        assertEquals(here, lattice.drumlin(SEED, x + (float) lap.skew(), z + (float) lap.zLap()),
                                CLOSE, lap + " at " + angle + ", " + x + " " + z);
                    }
                }
            }
        }
    }

    @Test
    void theFieldIsNotEmpty() {
        DrumlinLattice lattice = DrumlinLattice.over(LAPS[1], 0.6F, 0.8F);
        Random random = new Random(SEED);
        int raised = 0;
        for (int point = 0; point < POINTS; point++) {
            if (lattice.drumlin(SEED, random.nextFloat() * SPAN, random.nextFloat() * SPAN) > 0.0F) {
                raised++;
            }
        }

        assertTrue(raised > POINTS / 10, raised + " of " + POINTS + " points under a drumlin");
    }
}
