package com.exoticworlds.engine.seam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import com.exoticworlds.core.CoordinateConstants;
import com.exoticworlds.core.CuboidAtlas;
import com.exoticworlds.core.CuboidFace;
import com.exoticworlds.core.CuboidFixtures;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

class PoiSeamSearchTest {
    private static final WorldFold TORUS = WorldFolds.of(ShapeFixtures.torus(WorldLoopBounds.ofWidth(64)));
    private static final WorldFold NARROW = WorldFolds.of(ShapeFixtures.torus(WorldLoopBounds.ofWidth(4)));
    private static final int[] CUBOID_RADII = {1, 2, 5};
    private static final int WIDER_THAN_ANY_NET = 200;
    private static final int ROW_FROM_CORNER = 3;

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

    @Test
    void aSquareOnACuboidWalksEachChunkOnce() {
        for (CuboidAtlas atlas : CuboidFixtures.atlases()) {
            WorldFold fold = CuboidFixtures.cuboidFold(atlas);
            for (CuboidFace face : atlas.faces()) {
                for (ChunkPos center : chunksOf(face)) {
                    for (int radius : CUBOID_RADII) {
                        assertWalksEachChunkOnce(fold, center, radius);
                    }
                }
            }
        }
    }

    @Test
    void aSquareWiderThanTheCuboidWalksEachChunkOnce() {
        for (CuboidAtlas atlas : CuboidFixtures.atlases()) {
            CuboidFace front = CuboidFixtures.face(atlas, CuboidFace.Side.FRONT);

            assertWalksEachChunkOnce(CuboidFixtures.cuboidFold(atlas), chunksOf(front).getFirst(),
                    WIDER_THAN_ANY_NET);
        }
    }

    @Test
    void aSquareAcrossATurnedEdgeWalksTheChunksOfTheFaceBehindIt() {
        CuboidAtlas[] atlases = CuboidFixtures.atlases();
        CuboidAtlas atlas = atlases[atlases.length - 1];
        CuboidFace top = CuboidFixtures.face(atlas, CuboidFace.Side.TOP);
        ChunkPos center = new ChunkPos(top.minBlockX() / CoordinateConstants.CHUNK_WIDTH,
                top.minBlockZ() / CoordinateConstants.CHUNK_WIDTH + ROW_FROM_CORNER);

        List<ChunkPos> chunks = PoiSeamSearch.chunksAround(CuboidFixtures.cuboidFold(atlas), center, 1).toList();

        assertEquals(9, Set.copyOf(chunks).size());
        assertEquals(3, chunks.stream()
                .filter(chunk -> sideOf(atlas, chunk) == CuboidFace.Side.WEST)
                .count());
        assertEquals(6, chunks.stream()
                .filter(chunk -> sideOf(atlas, chunk) == CuboidFace.Side.TOP)
                .count());
    }

    private static void assertWalksEachChunkOnce(WorldFold fold, ChunkPos center, int radius) {
        List<ChunkPos> chunks = PoiSeamSearch.chunksAround(fold, center, radius).toList();

        assertEquals(chunks.size(), Set.copyOf(chunks).size(),
                () -> "a chunk is walked twice around " + center + " at radius " + radius);
    }

    private static List<ChunkPos> chunksOf(CuboidFace face) {
        List<ChunkPos> chunks = new ArrayList<>();
        for (int x = 0; x < face.widthBlocks(); x += CoordinateConstants.CHUNK_WIDTH) {
            for (int z = 0; z < face.depthBlocks(); z += CoordinateConstants.CHUNK_WIDTH) {
                chunks.add(ChunkPos.containing(new BlockPos(face.blockX(x), 0, face.blockZ(z))));
            }
        }

        return chunks;
    }

    private static CuboidFace.@Nullable Side sideOf(CuboidAtlas atlas, ChunkPos chunk) {
        CuboidFace face = atlas.faceAt(chunk.getMinBlockX(), chunk.getMinBlockZ());
        return face == null ? null : face.side();
    }
}
