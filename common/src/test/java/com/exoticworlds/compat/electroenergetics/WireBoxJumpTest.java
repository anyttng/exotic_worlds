package com.exoticworlds.compat.electroenergetics;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;

import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;

class WireBoxJumpTest {
    private static final WorldFold TINY_TORUS = WorldFolds.of(FlatShape.torus(WorldLoopBounds.ofWidth(32)));
    private static final WorldFold TINY_CYLINDER =
            WorldFolds.of(FlatShape.cylinder(WorldLoopBounds.ofWidth(Direction.Axis.X, 32)));
    private static final int CAPPED_RADIUS = 13;
    private static final ChunkPos START = new ChunkPos(2, 3);

    @Test
    void aOneChunkStepNeverReseats() {
        assertFalse(WireBoxJump.mayReseat(TINY_TORUS, START, CAPPED_RADIUS, new ChunkPos(3, 3), CAPPED_RADIUS));
    }

    @Test
    void aStepAcrossTheSeamIsOneChunk() {
        assertFalse(WireBoxJump.mayReseat(TINY_TORUS, new ChunkPos(15, 3), CAPPED_RADIUS, new ChunkPos(-16, 3),
                CAPPED_RADIUS));
    }

    @Test
    void aJumpReseatsFromTheLapLessBothRadii() {
        assertFalse(WireBoxJump.mayReseat(TINY_TORUS, START, CAPPED_RADIUS, new ChunkPos(-3, 3), CAPPED_RADIUS));
        assertTrue(WireBoxJump.mayReseat(TINY_TORUS, START, CAPPED_RADIUS, new ChunkPos(-4, 3), CAPPED_RADIUS));
        assertTrue(WireBoxJump.mayReseat(TINY_TORUS, START, CAPPED_RADIUS, new ChunkPos(2, 9), CAPPED_RADIUS));
    }

    @Test
    void anUnboundedAxisNeverReseats() {
        assertFalse(WireBoxJump.mayReseat(TINY_CYLINDER, START, CAPPED_RADIUS, new ChunkPos(2, 200), CAPPED_RADIUS));
        assertTrue(WireBoxJump.mayReseat(TINY_CYLINDER, START, CAPPED_RADIUS, new ChunkPos(-4, 3), CAPPED_RADIUS));
    }
}
