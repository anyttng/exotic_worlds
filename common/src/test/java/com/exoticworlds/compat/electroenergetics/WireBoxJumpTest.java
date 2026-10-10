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
    private static final WorldFold LATTICE = WorldFolds.of(FlatShape.latticeTorus(WorldLoopBounds.ofWidth(70), 37));
    private static final int LATTICE_RADIUS = 32;
    private static final ChunkPos ORIGIN = new ChunkPos(0, 0);

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
    void aStepAcrossTheSkewedSeamIsOneChunk() {
        assertFalse(WireBoxJump.mayReseat(LATTICE, new ChunkPos(5, 34), LATTICE_RADIUS, new ChunkPos(-32, -35),
                LATTICE_RADIUS));
    }

    @Test
    void aJumpOnTheLatticeReseatsWhereASecondCopyComesWithinBothRadii() {
        assertFalse(WireBoxJump.mayReseat(LATTICE, ORIGIN, LATTICE_RADIUS, new ChunkPos(0, 5), LATTICE_RADIUS));
        assertTrue(WireBoxJump.mayReseat(LATTICE, ORIGIN, LATTICE_RADIUS, new ChunkPos(0, 6), LATTICE_RADIUS));
    }

    @Test
    void anUnboundedAxisNeverReseats() {
        assertFalse(WireBoxJump.mayReseat(TINY_CYLINDER, START, CAPPED_RADIUS, new ChunkPos(2, 200), CAPPED_RADIUS));
        assertTrue(WireBoxJump.mayReseat(TINY_CYLINDER, START, CAPPED_RADIUS, new ChunkPos(-4, 3), CAPPED_RADIUS));
    }
}
