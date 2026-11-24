package com.exoticworlds.engine.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.core.world.WorldLoopBounds;

import net.minecraft.world.level.ChunkPos;

class RegionReachTest {
    private static final WorldFold TORUS = WorldFolds.of(ShapeFixtures.torus(WorldLoopBounds.ofWidth(64)));
    private static final ChunkPos CENTER = new ChunkPos(31, 0);
    private static final int REACH_CHUNKS = 2;

    @Test
    void aChunkInsideTheStepsReachKeepsItsOwnCoordinates() {
        assertNull(RegionReach.nearestPastTheReach(TORUS, CENTER, REACH_CHUNKS, 32, 0));
    }

    @Test
    void aChunkPastTheReachWithoutANearerCopyKeepsItsOwnCoordinates() {
        assertNull(RegionReach.nearestPastTheReach(TORUS, CENTER, REACH_CHUNKS, 28, 0));
    }

    @Test
    void aChunkPastTheReachTakesItsCopyNearestTheGeneratingChunk() {
        assertEquals(new ChunkPos(33, 0), RegionReach.nearestPastTheReach(TORUS, CENTER, REACH_CHUNKS, -31, 0));
    }

    @Test
    void anUnwrappedLevelNeverMovesAChunk() {
        assertNull(RegionReach.nearestPastTheReach(null, CENTER, REACH_CHUNKS, -31, 0));
    }
}
