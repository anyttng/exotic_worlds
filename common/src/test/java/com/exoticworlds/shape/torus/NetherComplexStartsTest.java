package com.exoticworlds.shape.torus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.core.world.WorldLoopBounds;

import net.minecraft.world.level.ChunkPos;

class NetherComplexStartsTest {
    private static final int WIDTH_CHUNKS = 16;
    private static final int SEPARATION = 4;
    private static final WorldFold SMALLEST_NETHER =
            WorldFolds.of(ShapeFixtures.torus(WorldLoopBounds.ofWidth(WIDTH_CHUNKS)));

    @Test
    void takesTheOriginWhenNothingStandsNearIt() {
        assertEquals(new ChunkPos(0, 0), NetherComplexStarts.firstFree(SMALLEST_NETHER, chunk -> true, List.of(),
                SEPARATION));
    }

    @Test
    void neverAsksAboutAChunkWithinTheSeparationThroughTheSeam() {
        List<ChunkPos> taken = List.of(new ChunkPos(7, 0), new ChunkPos(-2, -8));
        List<ChunkPos> asked = new ArrayList<>();

        ChunkPos chosen = NetherComplexStarts.firstFree(SMALLEST_NETHER, chunk -> {
            asked.add(chunk);
            return chunk.equals(new ChunkPos(-4, 4));
        }, taken, SEPARATION);

        assertEquals(new ChunkPos(-4, 4), chosen);
        assertFalse(asked.isEmpty());
        for (ChunkPos chunk : asked) {
            for (ChunkPos other : taken) {
                int gapChunks = Math.max(gapChunks(chunk.x(), other.x()), gapChunks(chunk.z(), other.z()));
                assertTrue(gapChunks >= SEPARATION, chunk + " was asked about, " + gapChunks + " chunks from " + other);
            }
        }
    }

    @Test
    void asksAboutEveryChunkOnceAndAnswersNullWhereNoneTakesIt() {
        List<ChunkPos> asked = new ArrayList<>();

        assertNull(NetherComplexStarts.firstFree(SMALLEST_NETHER, chunk -> {
            asked.add(chunk);
            return false;
        }, List.of(), SEPARATION));

        assertEquals(WIDTH_CHUNKS * WIDTH_CHUNKS, asked.size());
        assertEquals(WIDTH_CHUNKS * WIDTH_CHUNKS, new HashSet<>(asked).size());
        assertTrue(asked.stream().noneMatch(chunk -> chunk.x() < -WIDTH_CHUNKS / 2 || chunk.x() >= WIDTH_CHUNKS / 2
                || chunk.z() < -WIDTH_CHUNKS / 2 || chunk.z() >= WIDTH_CHUNKS / 2));
    }

    private static int gapChunks(int from, int to) {
        int gapChunks = Math.floorMod(to - from, WIDTH_CHUNKS);
        return Math.min(gapChunks, WIDTH_CHUNKS - gapChunks);
    }
}
