package com.exoticworlds.engine.seam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

class SeamTeleportTest {
    private static final int MIN_CHUNK = -16;
    private static final int MAX_CHUNK = 17;
    private static final double WIDTH_BLOCKS = (MAX_CHUNK - MIN_CHUNK) * 16.0;
    private static final WorldFold TORUS =
            WorldFolds.of(ShapeFixtures.torus(ShapeFixtures.loopedBounds(MIN_CHUNK, MAX_CHUNK, MIN_CHUNK, MAX_CHUNK)));
    private static final WorldFold CYLINDER_X = WorldFolds.of(ShapeFixtures.cylinder(WorldLoopBounds.UNBOUNDED.with(
            Direction.Axis.X, new WorldLoopBounds.AxisBounds.Looped(MIN_CHUNK, MAX_CHUNK))));
    private static final int LATTICE_WIDTH_CHUNKS = 32;
    private static final int SKEW_CHUNKS = 16;
    private static final WorldFold SKEWED =
            WorldFolds.of(FlatShape.latticeTorus(WorldLoopBounds.ofWidth(LATTICE_WIDTH_CHUNKS), SKEW_CHUNKS));

    private static final int VIEW_RADIUS = 14;
    private static final int NEAR_SEAM_CHUNK = 14;
    private static final int STEP_CHUNKS = 5;
    private static final int LAST_FLIPPED_CHUNK = 2;

    @Test
    void aTeleportInsideOneLapFlipsNoChunk() {
        ChunkTrackingView view = ChunkTrackingView.of(new ChunkPos(0, 0), VIEW_RADIUS);
        ChunkPos to = new ChunkPos(1, 1);

        assertEquals(List.of(), SeamTeleport.flipped(TORUS, new ChunkPos(0, 0), to, view));
        assertEquals(List.of(), SeamTeleport.flipped(CYLINDER_X, new ChunkPos(0, 0), to, view));
        assertEquals(List.of(), SeamTeleport.flipped(SKEWED, new ChunkPos(0, 0), to, view));
    }

    @Test
    void aTeleportAcrossTheSeamFlipsTheChunksPastTheNewHalfWorld() {
        ChunkPos from = new ChunkPos(NEAR_SEAM_CHUNK, 0);
        ChunkPos to = new ChunkPos(NEAR_SEAM_CHUNK + STEP_CHUNKS, 0);
        ChunkTrackingView view = ChunkTrackingView.of(from, VIEW_RADIUS);
        Set<ChunkPos> expected = viewChunks(view, pos -> pos.x() <= LAST_FLIPPED_CHUNK);

        assertEquals(expected, new HashSet<>(SeamTeleport.flipped(TORUS, from, to, view)));
        assertEquals(expected, new HashSet<>(SeamTeleport.flipped(CYLINDER_X, from, to, view)));
    }

    @Test
    void theCylinderFlipsNothingAlongItsOpenAxis() {
        ChunkPos from = new ChunkPos(0, NEAR_SEAM_CHUNK);
        ChunkPos to = new ChunkPos(0, NEAR_SEAM_CHUNK + STEP_CHUNKS);
        ChunkTrackingView view = ChunkTrackingView.of(from, VIEW_RADIUS);

        assertEquals(List.of(), SeamTeleport.flipped(CYLINDER_X, from, to, view));
        assertEquals(viewChunks(view, pos -> pos.z() <= LAST_FLIPPED_CHUNK),
                new HashSet<>(SeamTeleport.flipped(TORUS, from, to, view)));
    }

    @Test
    void aWholeLatticeLapFlipsEveryChunkInView() {
        ChunkPos from = new ChunkPos(0, 0);
        ChunkPos to = new ChunkPos(SKEW_CHUNKS, LATTICE_WIDTH_CHUNKS);
        ChunkTrackingView view = ChunkTrackingView.of(from, VIEW_RADIUS);

        assertEquals(viewChunks(view, pos -> true), new HashSet<>(SeamTeleport.flipped(SKEWED, from, to, view)));
    }

    @Test
    void anInBoundsDestinationComesBackAsTheSameInstance() {
        PositionMoveRotation destination = destinationAt(new Vec3(100.5, 64.0, -40.5));

        assertSame(destination, SeamTeleport.destination(TORUS, Vec3.ZERO, destination, Set.of()));
        assertSame(destination, SeamTeleport.destination(TORUS, Vec3.ZERO, destination, Relative.ALL));
    }

    @Test
    void anAbsoluteDestinationPastTheSeamIsFolded() {
        PositionMoveRotation destination = destinationAt(new Vec3(300.5, 64.0, -40.5));

        PositionMoveRotation folded = SeamTeleport.destination(TORUS, Vec3.ZERO, destination, Set.of());

        assertEquals(new Vec3(300.5 - WIDTH_BLOCKS, 64.0, -40.5), folded.position());
        assertEquals(destination.deltaMovement(), folded.deltaMovement());
        assertEquals(destination.yRot(), folded.yRot());
    }

    @Test
    void aRelativeOffsetFromAnAnchorPastTheSeamTakesTheLap() {
        PositionMoveRotation offset = destinationAt(new Vec3(2.0, 0.0, 0.0));
        Vec3 anchor = new Vec3(280.0, 64.0, 0.0);
        Set<Relative> relativeX = Set.of(Relative.X);

        PositionMoveRotation folded = SeamTeleport.destination(TORUS, anchor, offset, relativeX);

        assertEquals(new Vec3(2.0 - WIDTH_BLOCKS, 0.0, 0.0), folded.position());
    }

    private static PositionMoveRotation destinationAt(Vec3 position) {
        return new PositionMoveRotation(position, new Vec3(0.25, 0.0, 0.0), 90.0F, 10.0F);
    }

    private static Set<ChunkPos> viewChunks(ChunkTrackingView view, Predicate<ChunkPos> keep) {
        Set<ChunkPos> kept = new HashSet<>();
        view.forEach(pos -> {
            if (keep.test(pos)) {
                kept.add(pos);
            }
        });
        assertFalse(kept.isEmpty(), "the expected set holds no chunk in view, so the case grades nothing");
        return kept;
    }
}
