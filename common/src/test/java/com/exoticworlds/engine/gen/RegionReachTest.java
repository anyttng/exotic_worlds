package com.exoticworlds.engine.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;

import net.minecraft.world.level.ChunkPos;

class RegionReachTest {
    private static final WorldFold TORUS = WorldFolds.of(ShapeFixtures.torus(WorldLoopBounds.ofWidth(64)));
    private static final ChunkPos CENTER = new ChunkPos(31, 0);
    private static final int REACH = 2;

    @Test
    void aChunkInsideTheStepsReachKeepsItsOwnCoordinates() {
        assertNull(RegionReach.nearestPastTheReach(TORUS, CENTER, REACH, 32, 0));
    }

    @Test
    void aChunkPastTheReachWithoutANearerCopyKeepsItsOwnCoordinates() {
        assertNull(RegionReach.nearestPastTheReach(TORUS, CENTER, REACH, 28, 0));
    }

    @Test
    void aChunkPastTheReachTakesItsCopyNearestTheGeneratingChunk() {
        assertEquals(new ChunkPos(33, 0), RegionReach.nearestPastTheReach(TORUS, CENTER, REACH, -31, 0));
    }

    @Test
    void anUnwrappedLevelNeverMovesAChunk() {
        assertNull(RegionReach.nearestPastTheReach(null, CENTER, REACH, -31, 0));
    }
}
