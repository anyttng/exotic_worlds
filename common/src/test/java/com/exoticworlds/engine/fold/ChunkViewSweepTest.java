package com.exoticworlds.engine.fold;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.core.world.WorldLoopBounds;
import com.exoticworlds.core.world.WorldLoopBounds.AxisBounds;

import net.minecraft.world.level.ChunkPos;

class ChunkViewSweepTest {
    private static final int MIN_CHUNK = -16;
    private static final int MAX_CHUNK = 15;

    private static final WorldLoopBounds LOOPS_ON_X =
            new WorldLoopBounds(new AxisBounds.Looped(MIN_CHUNK, MAX_CHUNK + 1), AxisBounds.Unbounded.INSTANCE);
    private static final WorldFold CYLINDER = WorldFolds.of(ShapeFixtures.cylinder(LOOPS_ON_X));
    private static final WorldFold TORUS =
            WorldFolds.of(ShapeFixtures.torus(WorldLoopBounds.ofWidth(MAX_CHUNK - MIN_CHUNK + 1)));

    private static final int VIEW_DISTANCE_CHUNKS = 8;

    @Test
    void aPairTheFoldCannotRelateSweepsEachViewAloneAtAnyDistance() {
        for (int chunksAway : new int[] {1_250_000, 2_500_000}) {
            ChunkViewSweep sweep = sweepAlongTheUnboundedAxis(chunksAway);

            assertTrue(sweep instanceof ChunkViewSweep.EachAlone,
                    () -> "planned " + sweep + " for a pair " + chunksAway + " chunks apart");
        }
    }

    @Test
    void aMoveAcrossTheSeamPlansTheBoxItAlwaysDid() {
        ChunkViewSweep sweep = ChunkViewSweep.between(
                TORUS, new ChunkPos(15, 0), VIEW_DISTANCE_CHUNKS, new ChunkPos(-15, 0), VIEW_DISTANCE_CHUNKS);

        assertEquals(new ChunkViewSweep.OverBoth(6, -9, 26, 9), sweep);
    }

    @Test
    void noPairOfCentersInsideTheWorldEverSplitsTheSweep() {
        ChunkPos seamCorner = new ChunkPos(MAX_CHUNK, MAX_CHUNK);

        for (int x = MIN_CHUNK; x <= MAX_CHUNK; x++) {
            for (int z = MIN_CHUNK; z <= MAX_CHUNK; z++) {
                ChunkPos next = new ChunkPos(x, z);
                ChunkViewSweep sweep = ChunkViewSweep.between(TORUS, seamCorner, VIEW_DISTANCE_CHUNKS, next, VIEW_DISTANCE_CHUNKS);

                assertTrue(sweep instanceof ChunkViewSweep.OverBoth, () -> "split the sweep at " + next);
            }
        }
    }

    private static ChunkViewSweep sweepAlongTheUnboundedAxis(int chunksAway) {
        return ChunkViewSweep.between(
                CYLINDER, new ChunkPos(0, 0), VIEW_DISTANCE_CHUNKS, new ChunkPos(0, chunksAway), VIEW_DISTANCE_CHUNKS);
    }
}
