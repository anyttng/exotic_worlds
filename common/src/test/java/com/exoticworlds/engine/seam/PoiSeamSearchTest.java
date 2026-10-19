package com.exoticworlds.engine.seam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

class PoiSeamSearchTest {
    private static final WorldFold TORUS = WorldFolds.of(ShapeFixtures.torus(WorldLoopBounds.ofWidth(64)));
    private static final WorldFold NARROW = WorldFolds.of(ShapeFixtures.torus(WorldLoopBounds.ofWidth(4)));

    @Test
    void aSquareAcrossTheSeamWalksTheFoldedChunks() {
        Set<ChunkPos> chunks = PoiSeamSearch.chunksAround(TORUS, new ChunkPos(31, 0), 1)
                .collect(Collectors.toSet());

        assertEquals(9, chunks.size());
        assertTrue(chunks.contains(new ChunkPos(-32, 0)));
        assertFalse(chunks.contains(new ChunkPos(32, 0)));
    }

    @Test
    void aSquareWiderThanTheWorldWalksEachChunkOnce() {
        List<ChunkPos> chunks = PoiSeamSearch.chunksAround(NARROW, new ChunkPos(0, 0), 3).toList();

        assertEquals(16, chunks.size());
        assertEquals(16, Set.copyOf(chunks).size());
    }

    @Test
    void aRecordPastTheSeamSitsInTheSquareByItsNearestCopy() {
        BlockPos center = new BlockPos(500, 64, 0);
        BlockPos pastTheSeam = new BlockPos(-500, 64, 0);

        assertTrue(PoiSeamSearch.withinSquare(TORUS, center, 30, pastTheSeam));
        assertFalse(PoiSeamSearch.withinSquare(TORUS, center, 20, pastTheSeam));
    }

    @Test
    void theNearestRecordIsMeasuredThroughTheSeam() {
        BlockPos center = new BlockPos(500, 64, 0);
        BlockPos pastTheSeam = new BlockPos(-500, 64, 0);
        BlockPos origin = new BlockPos(0, 64, 0);

        assertEquals(pastTheSeam, List.of(origin, pastTheSeam).stream()
                .min(PoiSeamSearch.byDistance(TORUS, center))
                .orElseThrow());
    }
}
