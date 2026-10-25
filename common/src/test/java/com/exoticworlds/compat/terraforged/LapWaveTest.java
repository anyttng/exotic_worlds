package com.exoticworlds.compat.terraforged;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.core.Direction;

class LapWaveTest {
    private static final double LAP = 2048.0;

    private static final double NARROW_LAP = 1936.0;

    private static final double TINY_LAP = 512.0;

    private static final double SKEW = 272.0;

    private static final float[] WAVELENGTHS = {18.0F, 56.0F};

    private static final int DIRECTIONS = 72;

    private static final double CLOSE = 1.0E-9;

    private static final LapWave[] LAPS = {
        new LapWave(LAP, NARROW_LAP, RtfLap.NO_SKEW),
        new LapWave(LAP, LAP, SKEW),
        new LapWave(TINY_LAP, TINY_LAP, RtfLap.NO_SKEW),
        new LapWave(LAP, RtfLap.OPEN, RtfLap.NO_SKEW),
        new LapWave(RtfLap.OPEN, NARROW_LAP, RtfLap.NO_SKEW)
    };

    @Test
    void aRestatedWaveRunsAWholeNumberOfTimesAlongEveryLoopingLapVector() {
        for (LapWave lap : LAPS) {
            for (float wavelength : WAVELENGTHS) {
                for (int i = 0; i < DIRECTIONS; i++) {
                    double angle = 2.0 * Math.PI * i / DIRECTIONS;
                    LapWave.Wave wave = lap.noLonger(Math.cos(angle) / wavelength, Math.sin(angle) / wavelength);
                    if (lap.loops(Direction.Axis.X)) {
                        assertEquals((double) wave.xTurns(), wave.x() * lap.xLap(), CLOSE, lap + " at " + angle);
                    }

                    if (lap.loops(Direction.Axis.Z)) {
                        assertEquals((double) wave.zTurns(), wave.x() * lap.skew() + wave.z() * lap.zLap(), CLOSE,
                                lap + " at " + angle);
                    }
                }
            }
        }
    }

    @Test
    void aRestatedWavelengthIsNeverLongerThanTheModsOwn() {
        for (LapWave lap : LAPS) {
            for (float wavelength : WAVELENGTHS) {
                for (int i = 0; i < DIRECTIONS; i++) {
                    double angle = 2.0 * Math.PI * i / DIRECTIONS;
                    LapWave.Wave wave = lap.noLonger(Math.cos(angle) / wavelength, Math.sin(angle) / wavelength);
                    assertTrue(1.0 / wave.frequency() <= wavelength + CLOSE,
                            wavelength + " grew to " + 1.0 / wave.frequency() + " on " + lap + " at " + angle);
                }
            }
        }
    }

    @Test
    void anOpenAxisKeepsTheModsOwnComponent() {
        LapWave.Wave wave = LAPS[3].noLonger(0.01, 0.015);
        assertEquals(0.015, wave.z(), CLOSE);
        assertEquals(LapWave.NO_TURNS, wave.zTurns());
        LapWave.Wave across = LAPS[4].noLonger(0.01, 0.015);
        assertEquals(0.01, across.x(), CLOSE);
        assertEquals(LapWave.NO_TURNS, across.xTurns());
    }
}
