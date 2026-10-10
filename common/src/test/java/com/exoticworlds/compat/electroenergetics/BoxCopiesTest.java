package com.exoticworlds.compat.electroenergetics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;

class BoxCopiesTest {
    private static final WorldFold WIRE_BOX_LATTICE =
            WorldFolds.of(FlatShape.latticeTorus(WorldLoopBounds.ofWidth(70), 37));
    private static final WorldFold CAPTURE_LATTICE =
            WorldFolds.of(FlatShape.latticeTorus(WorldLoopBounds.ofWidth(16), 7));
    private static final WorldFold CAPTURE_TORUS = WorldFolds.of(FlatShape.torus(WorldLoopBounds.ofWidth(16)));
    private static final ChunkPos CENTRE = new ChunkPos(0, 0);
    private static final int RADIUS = 32;
    private static final BlockPos CAPTURE_ORIGIN = new BlockPos(-120, 0, -120);
    private static final Vec3i CAPTURE_SIZE = new Vec3i(240, 1, 240);
    private static final BlockPos CAPTURE_CENTRE = BlockPos.ZERO;

    @Test
    void aCornerChunkWhoseNearestCopyLeavesTheSquareStaysInside() {
        ChunkPos corner = new ChunkPos(RADIUS, RADIUS);
        assertNotEquals(corner, WIRE_BOX_LATTICE.nearestCopy(CENTRE, corner));
        assertEquals(corner, BoxCopies.seat(WIRE_BOX_LATTICE, CENTRE, RADIUS, corner));
    }

    @Test
    void aChunkInsideTheSquareCountsOnce() {
        assertEquals(1, BoxCopies.count(WIRE_BOX_LATTICE, CENTRE, 2 * RADIUS, new ChunkPos(0, 5)));
    }

    @Test
    void aJumpAcrossTheSkewedBandCountsThreeCopies() {
        assertEquals(3, BoxCopies.count(WIRE_BOX_LATTICE, CENTRE, 2 * RADIUS, new ChunkPos(0, 6)));
    }

    @Test
    void aCaptureCornerWhoseNearestCopyLeavesTheBoxStaysInside() {
        BlockPos corner = new BlockPos(119, 0, 119);
        assertEquals(new BlockPos(7, 0, -137), CAPTURE_LATTICE.nearestCopy(CAPTURE_CENTRE, corner));
        assertEquals(corner, BoxCopies.seat(CAPTURE_LATTICE, CAPTURE_ORIGIN, CAPTURE_SIZE, corner));
    }

    @Test
    void aCaptureCornerOnThePlainTorusIsItsNearestCopy() {
        BlockPos corner = new BlockPos(119, 0, 119);
        assertEquals(corner, BoxCopies.seat(CAPTURE_TORUS, CAPTURE_ORIGIN, CAPTURE_SIZE, corner));
    }
}
