package com.exoticworlds.compat.freeterraforged.banding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.exoticworlds.compat.freeterraforged.RtfLap;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;
import com.exoticworlds.engine.noise.ClimateScaleCompression;

import net.minecraft.core.Direction;

class BandingLatticeTest {
    private static final int NARROW_CHUNKS = 256;

    private static final int SKEW_CHUNKS = 37;

    private static final int REGION_BLOCKS = 300;

    private static final double COMPACT_FACTOR = 4.0;

    private static final int[] BLOCKS = {-4096, -2501, -17, 0, 913, 3333, 4095};

    private static final double CLOSE = 1.0E-3;

    @Test
    void aBlockAndItsCopyOneLapOnReadOneRegion() {
        WorldFold fold = torus();
        int lap = fold.blockDomain(Direction.Axis.X).domainLength;
        try (RtfLap.Frame.Scope bound = RtfLap.frame().bind(fold)) {
            RtfLap.Frame frame = RtfLap.frame();
            for (double compression : new double[] {ClimateScaleCompression.NO_COMPRESSION, COMPACT_FACTOR}) {
                for (int x : BLOCKS) {
                    for (int z : BLOCKS) {
                        assertEquals(BandingLattice.x(frame, compression, x, z, REGION_BLOCKS),
                                BandingLattice.x(frame, compression, x + lap, z, REGION_BLOCKS), CLOSE);
                        assertEquals(BandingLattice.z(frame, compression, z, REGION_BLOCKS),
                                BandingLattice.z(frame, compression, z + lap, REGION_BLOCKS), CLOSE);
                    }
                }
            }
        }
    }

    @Test
    void aBlockAndItsCopyOneSkewedLapOnReadOneRegion() {
        WorldFold fold = WorldFolds.of(new FlatShape(
                ShapeFixtures.loopedBounds(-NARROW_CHUNKS, NARROW_CHUNKS, -NARROW_CHUNKS, NARROW_CHUNKS), SKEW_CHUNKS, null));
        int lap = fold.blockLattice().z().domainLength;
        int skew = fold.blockLattice().skew();
        try (RtfLap.Frame.Scope bound = RtfLap.frame().bind(fold)) {
            RtfLap.Frame frame = RtfLap.frame();
            for (int x : BLOCKS) {
                for (int z : BLOCKS) {
                    double here = BandingLattice.x(frame, ClimateScaleCompression.NO_COMPRESSION, x, z, REGION_BLOCKS);
                    double copy = BandingLattice.x(frame, ClimateScaleCompression.NO_COMPRESSION, x + skew, z + lap,
                            REGION_BLOCKS);
                    int cellZ = (int) Math.floor(BandingLattice.z(frame, ClimateScaleCompression.NO_COMPRESSION, z,
                            REGION_BLOCKS));
                    int copyCellZ = (int) Math.floor(BandingLattice.z(frame, ClimateScaleCompression.NO_COMPRESSION,
                            z + lap, REGION_BLOCKS));
                    assertEquals(
                            BandingLattice.foldX(frame, ClimateScaleCompression.NO_COMPRESSION,
                                    (int) Math.floor(here), cellZ, REGION_BLOCKS),
                            BandingLattice.foldX(frame, ClimateScaleCompression.NO_COMPRESSION,
                                    (int) Math.floor(copy), copyCellZ, REGION_BLOCKS),
                            "the copy of " + x + " " + z + " one Z lap on lands in another region");
                }
            }
        }
    }

    @Test
    void aRegionIsNeverLargerThanFreeTerraForgedsAndShrinksWithCompactBiomes() {
        WorldFold fold = torus();
        int lap = fold.blockDomain(Direction.Axis.X).domainLength;
        try (RtfLap.Frame.Scope bound = RtfLap.frame().bind(fold)) {
            RtfLap.Frame frame = RtfLap.frame();
            double plain = lap / BandingLattice.x(frame, ClimateScaleCompression.NO_COMPRESSION, lap - 1, 0,
                    REGION_BLOCKS);
            double compact = lap / BandingLattice.x(frame, COMPACT_FACTOR, lap - 1, 0, REGION_BLOCKS);
            assertTrue(plain <= REGION_BLOCKS + CLOSE * lap, "a region grew to " + plain + " blocks");
            assertTrue(compact <= REGION_BLOCKS / COMPACT_FACTOR + CLOSE * lap,
                    "a region under Compact biomes stayed at " + compact + " blocks");
        }
    }

    @Test
    void aCellAndItsCopyOnePeriodOnHashAlike() {
        try (RtfLap.Frame.Scope bound = RtfLap.frame().bind(torus())) {
            RtfLap.Frame frame = RtfLap.frame();
            int period = frame.cells(Direction.Axis.X, 1.0 / REGION_BLOCKS);
            for (int cell = -period; cell <= period; cell++) {
                assertEquals(BandingLattice.foldX(frame, ClimateScaleCompression.NO_COMPRESSION, cell, 0, REGION_BLOCKS),
                        BandingLattice.foldX(frame, ClimateScaleCompression.NO_COMPRESSION, cell + period, 0,
                                REGION_BLOCKS));
                assertEquals(BandingLattice.foldZ(frame, ClimateScaleCompression.NO_COMPRESSION, cell, REGION_BLOCKS),
                        BandingLattice.foldZ(frame, ClimateScaleCompression.NO_COMPRESSION, cell + period,
                                REGION_BLOCKS));
            }
        }
    }

    @Test
    void anOpenAxisKeepsFreeTerraForgedsRegion() {
        WorldFold fold = WorldFolds.of(ShapeFixtures.cylinder(new WorldLoopBounds(
                new AxisBounds.Looped(-NARROW_CHUNKS, NARROW_CHUNKS), AxisBounds.Unbounded.INSTANCE)));
        try (RtfLap.Frame.Scope bound = RtfLap.frame().bind(fold)) {
            for (int z : BLOCKS) {
                assertEquals((double) z / REGION_BLOCKS, BandingLattice.z(RtfLap.frame(),
                        ClimateScaleCompression.NO_COMPRESSION, z, REGION_BLOCKS), CLOSE);
            }
        }
    }

    private static WorldFold torus() {
        return WorldFolds.of(ShapeFixtures.torus(
                ShapeFixtures.loopedBounds(-NARROW_CHUNKS, NARROW_CHUNKS, -NARROW_CHUNKS, NARROW_CHUNKS)));
    }
}
