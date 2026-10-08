package com.exoticworlds.compat.reterraforged;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;
import com.exoticworlds.engine.noise.GenerationTransformerContext;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.Direction;
import raccoonman.reterraforged.world.worldgen.feature.placement.poisson.FastPoisson;
import raccoonman.reterraforged.world.worldgen.feature.placement.poisson.FastPoissonContext;
import raccoonman.reterraforged.world.worldgen.feature.placement.poisson.LongIterSet;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.noise.module.Noises;
import raccoonman.reterraforged.world.worldgen.util.PosUtil;

class RtfDecorationTest {
    private static final int NARROW_CHUNKS = 16;

    private static final int WIDE_CHUNKS = 256;

    private static final int SEED = 1337;

    private static final int VARIANCE_SCALE = 100;

    private static final int LINES = 64;

    private static final int STRIP = 16;

    private static final float STEP_FACTOR = 4.0F;

    private static final double STEP_SLACK = 1.0E-5;

    private static final int POISSON_RADIUS = 7;

    private static final float POISSON_JITTER = 0.8F;

    private static final float POISSON_FREQUENCY = 0.3F;

    private static final int JOIN_REACH = 64;

    private static final int WARP_SAMPLES = 1024;

    private static final Noise VARIANCE = Noises.perlin(0, VARIANCE_SCALE, 1);

    private static final Noise WARPED = Noises.warpWhite(Noises.simplex(SEED, VARIANCE_SCALE, 2), SEED, 2, 4.0F);

    @FunctionalInterface
    private interface Sample {
        double at(double x, double z);
    }

    @BeforeAll
    static void activate() {
        RtfLap.activate();
    }

    @Test
    void aNoiseReadOutsideEveryScopeJoinsOnTheGenerationLap() {
        for (WorldFold fold : new WorldFold[] {torus(NARROW_CHUNKS), torus(WIDE_CHUNKS), cylinder(NARROW_CHUNKS)}) {
            GenerationTransformerContext.runWithTransformer(fold,
                    () -> assertEquals(0, jumps(fold, (x, z) -> VARIANCE.compute((float) x, (float) z, 0))));
        }
    }

    @Test
    void aWarpedNoiseReadsTheSameBitsOneLapOn() {
        WorldFold fold = torus(WIDE_CHUNKS);
        int min = fold.blockDomain(Direction.Axis.X).lowerBound;
        int lap = fold.blockDomain(Direction.Axis.X).domainLength;
        GenerationTransformerContext.runWithTransformer(fold, () -> {
            for (int x = min; x < min + lap; x += lap / WARP_SAMPLES) {
                assertEquals(WARPED.compute(x, min, 0), WARPED.compute(x + lap, min, 0), "at " + x);
            }
        });
    }

    @Test
    void anOpenedScopeKeepsItsLatticeOpen() {
        GenerationTransformerContext.runWithTransformer(torus(NARROW_CHUNKS), () -> {
            try (RtfLap.Frame.Scope open = RtfLap.frame().open()) {
                assertNull(RtfLap.boundFrame());
            }

            assertNotNull(RtfLap.boundFrame());
        });
    }

    @Test
    void theLapLeavesWithTheGenerationStep() {
        GenerationTransformerContext.runWithTransformer(torus(NARROW_CHUNKS), () -> assertNotNull(RtfLap.boundFrame()));
        assertNull(RtfLap.boundFrame());
    }

    @Test
    void poissonPointsOneLapOnAreThePointsShifted() {
        WorldFold fold = torus(NARROW_CHUNKS);
        int lapChunks = 2 * NARROW_CHUNKS;
        GenerationTransformerContext.runWithTransformer(fold, () -> {
            for (int chunk : new int[] {-NARROW_CHUNKS, NARROW_CHUNKS - 1}) {
                LongArrayList here = poisson(chunk);
                assertTrue(!here.isEmpty(), "chunk " + chunk + " holds a point");
                assertEquals(here, shifted(poisson(chunk + lapChunks), -lapChunks * 16));
            }
        });
    }

    @Test
    void poissonPointsOneLapOnDifferWhereNoLapIsBound() {
        int lapChunks = 2 * NARROW_CHUNKS;
        assertNotEquals(poisson(0), shifted(poisson(lapChunks), -lapChunks * 16));
    }

    @Test
    void aPoissonCellAtTheLatticeJoinIsAsWideAsTheOthers() {
        RtfLap.Frame frame = RtfLap.frame();
        try (RtfLap.Frame.Scope lap = frame.bind(torus(NARROW_CHUNKS))) {
            FastPoissonContext context = poissonContext();
            double width = frame.lap(Direction.Axis.X) / frame.cells(Direction.Axis.X, context.frequency());
            long previous = PoissonLattice.point(frame, SEED, -JOIN_REACH, 0, context);
            int run = 0;
            boolean interior = false;
            for (int x = -JOIN_REACH + 1; x < JOIN_REACH; x++) {
                long point = PoissonLattice.point(frame, SEED, x, 0, context);
                run++;
                if (point != previous) {
                    if (interior) {
                        assertTrue(run >= Math.floor(width) && run <= Math.ceil(width),
                                "a cell ending at " + x + " is " + run + " blocks wide against " + width);
                    }

                    interior = true;
                    run = 0;
                    previous = point;
                }
            }
        }
    }

    private static int jumps(WorldFold fold, Sample sample) {
        int jumps = 0;
        boolean varies = false;
        for (Direction.Axis axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
            if (!fold.blockDomain(axis).loops()) {
                continue;
            }

            Direction.Axis crossAxis = axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
            int min = fold.blockDomain(axis).lowerBound;
            int span = fold.blockDomain(axis).domainLength;
            boolean crossLoops = fold.blockDomain(crossAxis).loops();
            int crossMin = crossLoops ? fold.blockDomain(crossAxis).lowerBound : -span / 2;
            int crossSpan = crossLoops ? fold.blockDomain(crossAxis).domainLength : span;
            double[] values = new double[2 * STRIP + 1];
            for (int line = 0; line < LINES; line++) {
                int cross = crossMin + line * crossSpan / LINES;
                for (int i = 0; i <= 2 * STRIP; i++) {
                    int along = i < STRIP ? min + span - STRIP + i : min + i - STRIP;
                    values[i] = axis == Direction.Axis.X ? sample.at(along, cross) : sample.at(cross, along);
                }

                double seamStep = Math.abs(values[STRIP] - values[STRIP - 1]);
                double besideStep = 0.0;
                for (int i = 0; i < 2 * STRIP; i++) {
                    if (i != STRIP - 1) {
                        besideStep = Math.max(besideStep, Math.abs(values[i + 1] - values[i]));
                    }
                }

                varies |= besideStep > 0.0;
                if (seamStep > STEP_FACTOR * besideStep + STEP_SLACK) {
                    jumps++;
                }
            }
        }

        assertTrue(varies, "the sample never changes beside the seam");
        return jumps;
    }

    private static LongArrayList poisson(int chunkX) {
        LongArrayList placed = new LongArrayList();
        FastPoisson.visit(SEED, chunkX, 0, new Random(SEED), poissonContext(), new LongIterSet(), new LongArrayList(),
                placed, (x, z, list) -> list.add(PosUtil.pack(x, z)));
        return placed;
    }

    private static LongArrayList shifted(LongArrayList points, int dx) {
        LongArrayList moved = new LongArrayList(points.size());
        for (long point : points) {
            moved.add(PosUtil.pack(PosUtil.unpackLeft(point) + dx, PosUtil.unpackRight(point)));
        }

        return moved;
    }

    private static FastPoissonContext poissonContext() {
        return new FastPoissonContext(POISSON_RADIUS, POISSON_JITTER, POISSON_FREQUENCY, Noises.one());
    }

    private static WorldFold torus(int chunks) {
        return WorldFolds.of(FlatShape.torus(new WorldLoopBounds(-chunks, chunks, -chunks, chunks)));
    }

    private static WorldFold cylinder(int chunks) {
        return WorldFolds.of(FlatShape.cylinder(new WorldLoopBounds(new AxisBounds.Looped(-chunks, chunks),
                AxisBounds.Unbounded.INSTANCE)));
    }
}
