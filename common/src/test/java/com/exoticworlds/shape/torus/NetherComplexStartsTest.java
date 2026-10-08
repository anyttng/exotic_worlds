package com.exoticworlds.shape.torus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.TestShapes;
import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;

import net.minecraft.world.level.ChunkPos;

class NetherComplexStartsTest {
    private static final int WIDTH = 16;
    private static final int SEPARATION = 4;
    private static final ToroidalShape SMALLEST_NETHER =
            TestShapes.of(WorldFolds.of(FlatShape.torus(WorldLoopBounds.ofWidth(WIDTH))));

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
                int gap = Math.max(gap(chunk.x, other.x), gap(chunk.z, other.z));
                assertTrue(gap >= SEPARATION, chunk + " was asked about, " + gap + " chunks from " + other);
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

        assertEquals(WIDTH * WIDTH, asked.size());
        assertEquals(WIDTH * WIDTH, new HashSet<>(asked).size());
        assertTrue(asked.stream().noneMatch(chunk -> chunk.x < -WIDTH / 2 || chunk.x >= WIDTH / 2
                || chunk.z < -WIDTH / 2 || chunk.z >= WIDTH / 2));
    }

    private static int gap(int from, int to) {
        int gap = Math.floorMod(to - from, WIDTH);
        return Math.min(gap, WIDTH - gap);
    }
}
