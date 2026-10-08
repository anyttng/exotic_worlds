package com.exoticworlds.compat.reterraforged;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;

import net.minecraft.core.Direction;

class RtfLapTest {
    private static final int NARROW_CHUNKS = 256;

    private static final int WIDE_CHUNKS = 5000;

    private static final double CONTINENT_FREQUENCY = 1.0 / 12000.0;

    private static final double[] FREQUENCIES = {1.0 / 3.0, 1.0 / 97.0, 1.0 / 1500.0, CONTINENT_FREQUENCY, 1.0 / 90000.0};

    private static final double SIZE_EPSILON = 1.0E-9;

    @Test
    void aFeatureOnTheLapIsNeverLargerThanTheModsOwn() {
        for (WorldFold fold : new WorldFold[] {torus(NARROW_CHUNKS), torus(WIDE_CHUNKS), cylinder(NARROW_CHUNKS)}) {
            try (RtfLap.Frame.Scope lap = RtfLap.frame().bind(fold)) {
                RtfLap.Frame frame = RtfLap.frame();
                for (double frequency : FREQUENCIES) {
                    float snapped = frame.snappedFrequency(Direction.Axis.X, (float) frequency);
                    assertTrue(snapped >= (float) frequency * (1.0 - SIZE_EPSILON),
                            "a feature of frequency " + frequency + " grew to " + snapped);
                }
            }
        }
    }

    @Test
    void aLatticeClosesOnWholeCellsPerLap() {
        WorldFold fold = torus(NARROW_CHUNKS);
        int lap = fold.blockDomain(Direction.Axis.X).domainLength;
        try (RtfLap.Frame.Scope bound = RtfLap.frame().bind(fold)) {
            RtfLap.Frame frame = RtfLap.frame();
            int cells = frame.cells(Direction.Axis.X, CONTINENT_FREQUENCY);
            assertEquals(cells, lap * frame.snappedFrequency(Direction.Axis.X, (float) CONTINENT_FREQUENCY), 1.0E-4);
            assertTrue(cells >= 2, "a torus keeps at least two cells per lap");
        }
    }

    @Test
    void anOpenAxisKeepsTheModsOwnFrequency() {
        try (RtfLap.Frame.Scope bound = RtfLap.frame().bind(cylinder(NARROW_CHUNKS))) {
            RtfLap.Frame frame = RtfLap.frame();
            assertEquals(RtfLap.NO_PERIOD, frame.cells(Direction.Axis.Z, CONTINENT_FREQUENCY));
            assertEquals((float) CONTINENT_FREQUENCY,
                    frame.snappedFrequency(Direction.Axis.Z, (float) CONTINENT_FREQUENCY));
        }
    }

    @Test
    void aScaleCarriesTheLapIntoTheScaledUnits() {
        WorldFold fold = torus(NARROW_CHUNKS);
        int lap = fold.blockDomain(Direction.Axis.X).domainLength;
        try (RtfLap.Frame.Scope bound = RtfLap.frame().bind(fold);
                RtfLap.Frame.Scope scaled = RtfLap.frame().scale(0.5, 0.25)) {
            assertEquals(lap * 0.5, RtfLap.frame().lap(Direction.Axis.X));
            assertEquals(lap * 0.25, RtfLap.frame().lap(Direction.Axis.Z));
        }

        assertFalse(RtfLap.frame().bound());
    }

    @Test
    void aPointIsSeatedOnTheCopyNearestItsAnchor() {
        WorldFold fold = torus(NARROW_CHUNKS);
        int lap = fold.blockDomain(Direction.Axis.X).domainLength;
        try (RtfLap.Frame.Scope bound = RtfLap.frame().bind(fold)) {
            RtfLap.Frame frame = RtfLap.frame();
            assertEquals(100.0 + lap, frame.seat(Direction.Axis.X, 100.0, lap - 50.0));
            assertEquals(-100.0, frame.seat(Direction.Axis.X, -100.0 + lap, 0.0));
        }
    }

    @Test
    void aRiverNetworkFitsInsideHalfALap() {
        WorldFold fold = torus(NARROW_CHUNKS);
        int lap = fold.blockDomain(Direction.Axis.X).domainLength;
        try (RtfLap.Frame.Scope bound = RtfLap.frame().bind(fold)) {
            RtfLap.Frame frame = RtfLap.frame();
            float length = RiverReach.clamp(frame, Float.MAX_VALUE, 1.0F, 0.0F);
            assertTrue(length * 1.64F + 1024.0F <= lap / 2.0F + 1.0F, "a network of " + length + " leaves half a lap");
            assertTrue(RiverReach.fits(frame));
        }

        try (RtfLap.Frame.Scope bound = RtfLap.frame().bind(torus(32))) {
            assertFalse(RiverReach.fits(RtfLap.frame()), "a 1024-block world carries no rivers");
        }
    }

    private static WorldFold torus(int chunks) {
        return WorldFolds.of(FlatShape.torus(new WorldLoopBounds(-chunks, chunks, -chunks, chunks)));
    }

    private static WorldFold cylinder(int chunks) {
        return WorldFolds.of(FlatShape.cylinder(new WorldLoopBounds(new AxisBounds.Looped(-chunks, chunks),
                AxisBounds.Unbounded.INSTANCE)));
    }
}
