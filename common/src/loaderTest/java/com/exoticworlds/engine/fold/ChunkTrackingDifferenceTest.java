package com.exoticworlds.engine.fold;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.exoticworlds.accessors.FoldHolder;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.core.world.WorldLoopBounds;
import com.exoticworlds.core.world.WorldLoopBounds.AxisBounds;

import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.world.level.ChunkPos;

@Timeout(60)
class ChunkTrackingDifferenceTest {
    private static final int MIN_CHUNK = -16;
    private static final int MAX_CHUNK = 15;
    private static final int VIEW_DISTANCE_CHUNKS = 8;
    private static final int NEIGHBOR_BUFFER = 2;

    private static final WorldFold CYLINDER = WorldFolds.of(ShapeFixtures.cylinder(
            new WorldLoopBounds(new AxisBounds.Looped(MIN_CHUNK, MAX_CHUNK + 1), AxisBounds.Unbounded.INSTANCE)));
    private static final WorldFold TORUS =
            WorldFolds.of(ShapeFixtures.torus(WorldLoopBounds.ofWidth(MAX_CHUNK - MIN_CHUNK + 1)));

    @Test
    void aJumpAlongTheUnboundedAxisEntersTheDestinationAndLeavesTheOrigin() {
        ChunkTrackingView.Positioned previous = view(CYLINDER, 0, 0);
        ChunkTrackingView.Positioned next = view(CYLINDER, 0, 1_250_000);

        Set<ChunkPos> entered = new HashSet<>();
        Set<ChunkPos> left = new HashSet<>();
        ChunkTrackingView.difference(previous, next, entered::add, left::add);

        assertEquals(viewChunks(CYLINDER, 0, 1_250_000), entered);
        assertEquals(viewChunks(CYLINDER, 0, 0), left);
    }

    @Test
    void aStepAcrossTheSeamEntersAndLeavesOnlyWhatTheTwoViewsDoNotShare() {
        ChunkTrackingView.Positioned previous = view(TORUS, 15, 0);
        ChunkTrackingView.Positioned next = view(TORUS, -15, 0);

        Set<ChunkPos> entered = new HashSet<>();
        Set<ChunkPos> left = new HashSet<>();
        ChunkTrackingView.difference(previous, next, entered::add, left::add);

        Set<ChunkPos> previousChunks = viewChunks(TORUS, 15, 0);
        Set<ChunkPos> nextChunks = viewChunks(TORUS, -15, 0);

        assertEquals(without(nextChunks, previousChunks), entered);
        assertEquals(without(previousChunks, nextChunks), left);
    }

    @Test
    void aRawPositionAcrossTheSeamIsTrackedAsItsPhysicalChunk() {
        ChunkTrackingView.Positioned view = view(TORUS, MAX_CHUNK, 0);

        assertTrue(view.contains(MAX_CHUNK + 1, 0), "the raw neighbor across the seam is refused");
        assertEquals(view.contains(MIN_CHUNK + 2, 0), view.contains(MAX_CHUNK + 3, 0));
        assertFalse(view.contains(MAX_CHUNK + VIEW_DISTANCE_CHUNKS + NEIGHBOR_BUFFER + 1, 0));
    }

    @Test
    void forEachOverTheSeamVisitsEachPhysicalChunkOnce() {
        List<ChunkPos> visited = new ArrayList<>();
        view(TORUS, MAX_CHUNK, 0).forEach(visited::add);

        assertEquals(viewChunks(TORUS, MAX_CHUNK, 0), new HashSet<>(visited));
        assertEquals(visited.size(), new HashSet<>(visited).size(), "a physical chunk was visited twice");
    }

    private static ChunkTrackingView.Positioned view(WorldFold fold, int centerX, int centerZ) {
        ChunkTrackingView.Positioned view =
                (ChunkTrackingView.Positioned) ChunkTrackingView.of(new ChunkPos(centerX, centerZ), VIEW_DISTANCE_CHUNKS);
        ((FoldHolder) (Object) view).exotic_worlds$setFold(fold);

        assertSame(fold, ((FoldHolder) (Object) view).exotic_worlds$getFold(), "the view kept no fold");
        return view;
    }

    private static Set<ChunkPos> viewChunks(WorldFold fold, int centerX, int centerZ) {
        Set<ChunkPos> chunks = new HashSet<>();
        int radiusChunks = VIEW_DISTANCE_CHUNKS + 1;

        for (int x = centerX - radiusChunks; x <= centerX + radiusChunks; x++) {
            for (int z = centerZ - radiusChunks; z <= centerZ + radiusChunks; z++) {
                long deltaX = Math.max(0, Math.abs(x - centerX) - NEIGHBOR_BUFFER);
                long deltaZ = Math.max(0, Math.abs(z - centerZ) - NEIGHBOR_BUFFER);
                if (deltaX * deltaX + deltaZ * deltaZ < (long) VIEW_DISTANCE_CHUNKS * VIEW_DISTANCE_CHUNKS) {
                    chunks.add(fold.fold(new ChunkPos(x, z)));
                }
            }
        }

        return chunks;
    }

    private static Set<ChunkPos> without(Set<ChunkPos> chunks, Set<ChunkPos> removed) {
        Set<ChunkPos> rest = new HashSet<>(chunks);
        rest.removeAll(removed);
        return rest;
    }
}
