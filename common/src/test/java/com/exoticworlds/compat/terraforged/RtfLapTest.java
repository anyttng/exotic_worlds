package com.exoticworlds.compat.terraforged;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;

import net.minecraft.core.Direction;

class RtfLapTest {
    private static final int NARROW_CHUNKS = 256;

    private static final int WIDE_CHUNKS = 5000;

    private static final int CONTINENT_SCALE = 3000;

    private static final double CONTINENT_FREQUENCY = 1.0 / (CONTINENT_SCALE * 4.0);

    private static final double[] FREQUENCIES = {1.0 / 3.0, 1.0 / 97.0, 1.0 / 1500.0, CONTINENT_FREQUENCY, 1.0 / 90000.0};

    private static final double SIZE_EPSILON = 1.0E-9;

    private static final int SKEW_CHUNKS = 37;

    private static final float SEAM_STEP = 0.01F;

    private static final double CLOSE = 1.0E-3;

    private static final float[] ALONG_FRACTIONS = {0.0F, 0.3F, 0.71F};

    private static final int CELL_REACH = 2;

    @Test
    void aFeatureOnTheLapIsNeverLargerThanTheModsOwn() {
        for (WorldFold fold : new WorldFold[] {torus(NARROW_CHUNKS), torus(WIDE_CHUNKS),
                cylinder(Direction.Axis.X, NARROW_CHUNKS), cylinder(Direction.Axis.Z, NARROW_CHUNKS)}) {
            try (RtfLap.Frame.Scope bound = RtfLap.frame().bind(fold)) {
                RtfLap.Frame frame = RtfLap.frame();
                for (Direction.Axis axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
                    for (double frequency : FREQUENCIES) {
                        float snapped = frame.snappedFrequency(axis, (float) frequency);
                        assertTrue(snapped >= (float) frequency * (1.0 - SIZE_EPSILON),
                                "a feature of frequency " + frequency + " grew to " + snapped + " on " + axis);
                    }
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
    void anOpenAxisShrinksByTheLoopingAxisFactorOnACylinder() {
        for (Direction.Axis loop : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
            Direction.Axis open = loop == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
            try (RtfLap.Frame.Scope bound = RtfLap.frame().bind(cylinder(loop, NARROW_CHUNKS))) {
                RtfLap.Frame frame = RtfLap.frame();
                for (double frequency : FREQUENCIES) {
                    assertEquals(RtfLap.NO_PERIOD, frame.cells(open, frequency));
                    double factor = frame.snappedFrequency(loop, (float) frequency) / frequency;
                    assertEquals(frequency * factor, frame.snappedFrequency(open, (float) frequency),
                            frequency * factor * SIZE_EPSILON, "looping on " + loop + " at frequency " + frequency);
                    assertEquals(frame.cellRatio(loop, frequency), frame.cellRatio(open, frequency),
                            "looping on " + loop + " at frequency " + frequency);
                    assertEquals(frame.snappedAngular(loop, (float) frequency),
                            frame.snappedAngular(open, (float) frequency),
                            "looping on " + loop + " at frequency " + frequency);
                    try (RtfLap.Frame.Scope lattice = frame.octave(frequency)) {
                        float loopScale = loop == Direction.Axis.X ? frame.xScale() : frame.zScale();
                        float openScale = loop == Direction.Axis.X ? frame.zScale() : frame.xScale();
                        assertEquals(loopScale, openScale, "looping on " + loop + " at frequency " + frequency);
                    }
                }
            }
        }
    }

    @Test
    void aTorusSnapsEachAxisOnItsOwnLap() {
        WorldFold fold = skewed(NARROW_CHUNKS);
        try (RtfLap.Frame.Scope bound = RtfLap.frame().bind(fold)) {
            RtfLap.Frame frame = RtfLap.frame();
            for (Direction.Axis axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
                double lap = frame.lap(axis);
                for (double frequency : FREQUENCIES) {
                    int cells = frame.cells(axis, frequency);
                    assertEquals((float) (cells / lap), frame.snappedFrequency(axis, (float) frequency),
                            axis + " at frequency " + frequency);
                    assertEquals(lap * frequency / cells, frame.cellRatio(axis, frequency),
                            axis + " at frequency " + frequency);
                }
            }
        }
    }

    @Test
    void anUnshapedWorldKeepsTheModsOwnFrequency() {
        RtfLap.Frame frame = RtfLap.frame();
        for (Direction.Axis axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
            for (double frequency : FREQUENCIES) {
                assertEquals((float) frequency, frame.snappedFrequency(axis, (float) frequency));
                assertEquals(1.0, frame.cellRatio(axis, frequency));
                assertEquals((float) frequency, frame.snappedAngular(axis, (float) frequency));
            }
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
            assertEquals(100.0 + lap, frame.seatX(100.0, 0.0, lap - 50.0, 0.0));
            assertEquals(-100.0, frame.seatX(-100.0 + lap, 0.0, 0.0, 0.0));
        }
    }

    @Test
    void aSkewedLatticeBindsTheFrameWithItsSkew() {
        WorldFold fold = skewed(NARROW_CHUNKS);
        try (RtfLap.Frame.Scope bound = RtfLap.frame().bind(fold)) {
            RtfLap.Frame frame = RtfLap.frame();
            assertEquals(fold.blockLattice().x().domainLength, frame.lap(Direction.Axis.X));
            assertEquals(fold.blockLattice().z().domainLength, frame.lap(Direction.Axis.Z));
            assertEquals(fold.blockLattice().skew(), frame.skew());
        }

        assertEquals(RtfLap.NO_SKEW, RtfLap.frame().skew());
    }

    @Test
    void aCellOneZLapOnFoldsAsTheCellItCopies() {
        try (RtfLap.Frame.Scope bound = RtfLap.frame().bind(skewed(NARROW_CHUNKS))) {
            RtfLap.Frame frame = RtfLap.frame();
            for (double frequency : FREQUENCIES) {
                try (RtfLap.Frame.Scope lattice = frame.octave(frequency)) {
                    int xPeriod = frame.cells(Direction.Axis.X, frequency);
                    int zPeriod = frame.cells(Direction.Axis.Z, frequency);
                    int skewCells = (int) Math.round(frame.skew() * frame.xScale());
                    for (int cellZ = -CELL_REACH; cellZ <= CELL_REACH; cellZ++) {
                        for (int cellX = -CELL_REACH; cellX <= CELL_REACH; cellX++) {
                            int folded = frame.foldX(cellX, cellZ);
                            assertEquals(folded, frame.foldX(cellX + skewCells, cellZ + zPeriod));
                            assertEquals(folded, frame.foldX(cellX + xPeriod, cellZ));
                            assertEquals(frame.foldZ(cellZ), frame.foldZ(cellZ + zPeriod));
                        }
                    }
                }
            }
        }
    }

    @Test
    void theSkewedSeamStepsByAWholeCellLatticeVector() {
        WorldFold fold = skewed(NARROW_CHUNKS);
        int xLap = fold.blockLattice().x().domainLength;
        float seam = fold.blockLattice().z().domainLength;
        try (RtfLap.Frame.Scope bound = RtfLap.frame().bind(fold)) {
            RtfLap.Frame frame = RtfLap.frame();
            for (double frequency : FREQUENCIES) {
                try (RtfLap.Frame.Scope lattice = frame.octave(frequency)) {
                    int xPeriod = frame.cells(Direction.Axis.X, frequency);
                    int zPeriod = frame.cells(Direction.Axis.Z, frequency);
                    int skewCells = (int) Math.round(frame.skew() * frame.xScale());
                    for (float fraction : ALONG_FRACTIONS) {
                        float x = fraction * xLap;
                        double uStep = (frame.latticeX(x, seam - SEAM_STEP) - frame.latticeX(x, seam + SEAM_STEP))
                                * (double) frame.xScale() - skewCells;
                        double vStep = (frame.shiftZ(seam - SEAM_STEP) - frame.shiftZ(seam + SEAM_STEP))
                                * (double) frame.zScale() - zPeriod;
                        double uResidual = uStep - xPeriod * Math.rint(uStep / xPeriod);
                        assertEquals(0.0, uResidual, CLOSE + 2.0 * SEAM_STEP * frame.xScale(),
                                "x " + x + " at frequency " + frequency);
                        assertEquals(0.0, vStep, CLOSE + 2.0 * SEAM_STEP * frame.zScale(),
                                "x " + x + " at frequency " + frequency);
                    }
                }
            }
        }
    }

    @Test
    void aLatticePositionReadsBackAsTheBlockItCameFrom() {
        WorldFold fold = skewed(NARROW_CHUNKS);
        int xLap = fold.blockLattice().x().domainLength;
        int zLap = fold.blockLattice().z().domainLength;
        try (RtfLap.Frame.Scope bound = RtfLap.frame().bind(fold)) {
            RtfLap.Frame frame = RtfLap.frame();
            for (double frequency : FREQUENCIES) {
                try (RtfLap.Frame.Scope lattice = frame.octave(frequency)) {
                    float x = ALONG_FRACTIONS[1] * xLap;
                    float z = ALONG_FRACTIONS[2] * zLap;
                    assertEquals(x, frame.outerX(frame.latticeX(x, z), frame.shiftZ(z)), CLOSE,
                            "at frequency " + frequency);
                }
            }
        }
    }

    @Test
    void aPointOneZLapFromItsAnchorIsSeatedOnTheAnchorsCopy() {
        WorldFold fold = skewed(NARROW_CHUNKS);
        int zLap = fold.blockLattice().z().domainLength;
        int skew = fold.blockLattice().skew();
        try (RtfLap.Frame.Scope bound = RtfLap.frame().bind(fold)) {
            RtfLap.Frame frame = RtfLap.frame();
            assertEquals(200.0, frame.seatZ(200.0 + zLap, 200.0));
            assertEquals(100.0, frame.seatX(100.0 + skew, 200.0 + zLap, 100.0, 200.0));
            assertEquals(100.0, frame.seatX(100.0 - skew, 200.0 - zLap, 100.0, 200.0));
        }
    }

    private static WorldFold skewed(int chunks) {
        return WorldFolds.of(new FlatShape(ShapeFixtures.loopedBounds(-chunks, chunks, -chunks, chunks), SKEW_CHUNKS, null));
    }

    private static WorldFold torus(int chunks) {
        return WorldFolds.of(ShapeFixtures.torus(ShapeFixtures.loopedBounds(-chunks, chunks, -chunks, chunks)));
    }

    private static WorldFold cylinder(Direction.Axis loop, int chunks) {
        AxisBounds looped = new AxisBounds.Looped(-chunks, chunks);
        return WorldFolds.of(ShapeFixtures.cylinder(loop == Direction.Axis.X
                ? new WorldLoopBounds(looped, AxisBounds.Unbounded.INSTANCE)
                : new WorldLoopBounds(AxisBounds.Unbounded.INSTANCE, looped)));
    }
}
