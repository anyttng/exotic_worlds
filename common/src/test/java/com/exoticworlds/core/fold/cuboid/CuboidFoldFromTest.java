package com.exoticworlds.core.fold.cuboid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.exoticworlds.compat.CompatFoldFixture;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.world.CuboidShape;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;

class CuboidFoldFromTest {
    private static final int GENERATION_RADIUS_CHUNKS = 11;
    private static final int NEIGHBOR_STEP_CHUNKS = 1;
    private static final int SOURCE_RADIUS_CHUNKS = 8;
    private static final int TORUS_REACH_CHUNKS = 40;
    private static final int TORUS_STEP_CHUNKS = 7;
    private static final int SECTION_Y = -3;
    private static final int REF_SECTION_Y = 5;
    private static final WorldFold CUBE_32 = CuboidFixtures.cuboidFold(new CuboidShape(32, 32, 32).atlas());
    private static final List<WorldFold> PLAYABLE = List.of(
            CuboidFixtures.cuboidFold(new CuboidShape(16, 16, 16).atlas()),
            CuboidFixtures.cuboidFold(new CuboidShape(16, 24, 40).atlas()));

    private static long key(int chunkX, int chunkZ) {
        return ChunkPos.pack(chunkX, chunkZ);
    }

    private static boolean isOnNet(WorldFold fold, long chunkKey) {
        return !fold.isOver(ChunkPos.unpack(chunkKey));
    }

    private static List<Long> netChunksBesideAnEdge(WorldFold fold) {
        List<Long> chunks = new ArrayList<>();
        for (CuboidFace face : fold.getCuboidAtlas().faces()) {
            int minChunkX = SectionPos.blockToSectionCoord(face.minBlockX());
            int minChunkZ = SectionPos.blockToSectionCoord(face.minBlockZ());
            int widthChunks = SectionPos.blockToSectionCoord(face.widthBlocks());
            int depthChunks = SectionPos.blockToSectionCoord(face.depthBlocks());
            for (int x = 0; x < widthChunks; x++) {
                for (int z = 0; z < depthChunks; z++) {
                    int fromEdge = Math.min(Math.min(x, widthChunks - 1 - x), Math.min(z, depthChunks - 1 - z));
                    if (fromEdge < GENERATION_RADIUS_CHUNKS) {
                        chunks.add(key(minChunkX + x, minChunkZ + z));
                    }
                }
            }
        }

        return chunks;
    }

    private static List<Long> neighborsOf(WorldFold fold, long chunkKey) {
        List<Long> neighbors = new ArrayList<>();
        int x = ChunkPos.getX(chunkKey);
        int z = ChunkPos.getZ(chunkKey);
        for (int offsetX = -NEIGHBOR_STEP_CHUNKS; offsetX <= NEIGHBOR_STEP_CHUNKS; offsetX++) {
            for (int offsetZ = -NEIGHBOR_STEP_CHUNKS; offsetZ <= NEIGHBOR_STEP_CHUNKS; offsetZ++) {
                if (offsetX != 0 || offsetZ != 0) {
                    neighbors.add(fold.foldChunkKeyFrom(chunkKey, key(x + offsetX, z + offsetZ)));
                }
            }
        }

        return neighbors;
    }

    private static Map<Long, Integer> graphDistances(WorldFold fold, long source) {
        Map<Long, Integer> distances = new HashMap<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        distances.put(source, 0);
        queue.add(source);
        while (!queue.isEmpty()) {
            long chunk = queue.poll();
            int distance = distances.get(chunk);
            if (distance == GENERATION_RADIUS_CHUNKS) {
                continue;
            }

            for (long neighbor : neighborsOf(fold, chunk)) {
                if (distances.putIfAbsent(neighbor, distance + 1) == null) {
                    queue.add(neighbor);
                }
            }
        }

        return distances;
    }

    @Test
    void aSlotOnTheDiagonalOfAnEmptyCornerTakesTheEdgeItsLineCrosses() {
        assertEquals(key(-7, -26), CUBE_32.foldChunkKeyFrom(key(-33, -16), key(-26, -26)));
    }

    @Test
    void aSlotPastAVertexFollowsItsLineOverTwoEdges() {
        assertEquals(key(69, -7), CUBE_32.foldChunkKeyFrom(key(-47, -16), key(-58, -27)));
    }

    @Test
    void aChunkOnTheNetComesBackAsGiven() {
        assertEquals(key(-20, -16), CUBE_32.foldChunkKeyFrom(key(-33, -16), key(-20, -16)));
    }

    @Test
    void everySlotOfAGenerationSquareLandsOnAFaceTheGraphReachesInTime() {
        for (WorldFold fold : PLAYABLE) {
            for (long center : netChunksBesideAnEdge(fold)) {
                Map<Long, Integer> distances = graphDistances(fold, center);
                int centerX = ChunkPos.getX(center);
                int centerZ = ChunkPos.getZ(center);
                for (int x = centerX - GENERATION_RADIUS_CHUNKS; x <= centerX + GENERATION_RADIUS_CHUNKS; x++) {
                    for (int z = centerZ - GENERATION_RADIUS_CHUNKS; z <= centerZ + GENERATION_RADIUS_CHUNKS; z++) {
                        long slot = fold.foldChunkKeyFrom(center, key(x, z));
                        int ringChunks = Math.max(Math.abs(x - centerX), Math.abs(z - centerZ));
                        String where = "slot " + x + "," + z + " of " + ChunkPos.unpack(center);

                        assertTrue(isOnNet(fold, slot), where);
                        assertTrue(distances.getOrDefault(slot, Integer.MAX_VALUE) <= ringChunks, where);
                    }
                }
            }
        }
    }

    @Test
    void everyNeighborOfANetChunkIsOnTheNetAndSeesItBack() {
        for (WorldFold fold : PLAYABLE) {
            for (long chunk : netChunksBesideAnEdge(fold)) {
                for (long neighbor : neighborsOf(fold, chunk)) {
                    String where = ChunkPos.unpack(neighbor) + " beside " + ChunkPos.unpack(chunk);

                    assertTrue(isOnNet(fold, neighbor), where);
                    assertTrue(neighborsOf(fold, neighbor).contains(chunk), where);
                }
            }
        }
    }

    @Test
    void theNeighborAcrossTheDiagonalOfAnEmptyCornerIsTheChunkPastTheVertex() {
        assertFalse(isOnNet(CUBE_32, CUBE_32.foldChunkKey(key(-17, -17))));
        assertEquals(key(-16, -17), CUBE_32.foldChunkKeyFrom(key(-17, -16), key(-17, -17)));
        assertEquals(key(-17, -16), CUBE_32.foldChunkKeyFrom(key(-16, -17), key(-17, -17)));
    }

    @Test
    void theChunkPastTheVertexFoldsAlikeAsAChunkPosAndAsASection() {
        assertEquals(new ChunkPos(-16, -17), CUBE_32.foldFrom(new ChunkPos(-17, -16), new ChunkPos(-17, -17)));
        assertEquals(SectionPos.asLong(-16, SECTION_Y, -17),
                CUBE_32.foldSectionNodeFrom(SectionPos.asLong(-17, REF_SECTION_Y, -16),
                        SectionPos.asLong(-17, SECTION_Y, -17)));
    }

    @Test
    void everyNeighborFoldsAsAChunkPosAndAsASectionWhereItsKeyFolds() {
        for (WorldFold fold : PLAYABLE) {
            for (long chunk : netChunksBesideAnEdge(fold)) {
                ChunkPos center = ChunkPos.unpack(chunk);
                long centerSection = SectionPos.asLong(center.x(), REF_SECTION_Y, center.z());
                for (int offsetX = -NEIGHBOR_STEP_CHUNKS; offsetX <= NEIGHBOR_STEP_CHUNKS; offsetX++) {
                    for (int offsetZ = -NEIGHBOR_STEP_CHUNKS; offsetZ <= NEIGHBOR_STEP_CHUNKS; offsetZ++) {
                        ChunkPos neighbor = new ChunkPos(center.x() + offsetX, center.z() + offsetZ);
                        long folded = fold.foldChunkKeyFrom(chunk, neighbor.pack());
                        long section = fold.foldSectionNodeFrom(centerSection,
                                SectionPos.asLong(neighbor.x(), SECTION_Y, neighbor.z()));
                        String where = neighbor + " beside " + center;

                        assertEquals(ChunkPos.unpack(folded), fold.foldFrom(center, neighbor), where);
                        assertEquals(SectionPos.asLong(ChunkPos.getX(folded), SECTION_Y, ChunkPos.getZ(folded)),
                                section, where);
                    }
                }
            }
        }
    }

    @Test
    void everyChunkOfASquareIsCarriedOntoTheChunkItFoldsTo() {
        for (WorldFold fold : PLAYABLE) {
            for (long chunk : netChunksBesideAnEdge(fold)) {
                ChunkPos center = ChunkPos.unpack(chunk);
                for (int x = center.x() - SOURCE_RADIUS_CHUNKS; x <= center.x() + SOURCE_RADIUS_CHUNKS; x++) {
                    for (int z = center.z() - SOURCE_RADIUS_CHUNKS; z <= center.z() + SOURCE_RADIUS_CHUNKS; z++) {
                        ChunkPos raw = new ChunkPos(x, z);

                        assertEquals(fold.foldFrom(center, raw), fold.deckTransformationFrom(center, raw).apply(raw),
                                raw + " of " + center);
                    }
                }
            }
        }
    }

    @Test
    void theChunkPastTheVertexIsCarriedOverTheEdgeItsLineCrosses() {
        ChunkPos diagonal = new ChunkPos(-17, -17);

        assertEquals(new ChunkPos(-16, -17),
                CUBE_32.deckTransformationFrom(new ChunkPos(-17, -16), diagonal).apply(diagonal));
        assertEquals(new ChunkPos(-17, -16),
                CUBE_32.deckTransformationFrom(new ChunkPos(-16, -17), diagonal).apply(diagonal));
    }

    @Test
    void aTorusFoldsFromAnyChunkAsItFoldsAlone() {
        for (WorldFold fold : List.of(CompatFoldFixture.PER_AXIS, CompatFoldFixture.DECK_TORUS)) {
            long ref = key(0, 0);
            ChunkPos refPos = ChunkPos.unpack(ref);
            long refSection = SectionPos.asLong(0, REF_SECTION_Y, 0);
            for (int x = -TORUS_REACH_CHUNKS; x <= TORUS_REACH_CHUNKS; x += TORUS_STEP_CHUNKS) {
                for (int z = -TORUS_REACH_CHUNKS; z <= TORUS_REACH_CHUNKS; z += TORUS_STEP_CHUNKS) {
                    long section = SectionPos.asLong(x, SECTION_Y, z);

                    assertEquals(fold.foldChunkKey(key(x, z)), fold.foldChunkKeyFrom(ref, key(x, z)));
                    assertEquals(fold.fold(new ChunkPos(x, z)), fold.foldFrom(refPos, new ChunkPos(x, z)));
                    assertEquals(fold.foldSectionNode(section), fold.foldSectionNodeFrom(refSection, section));
                    assertEquals(fold.deckTransformation(new ChunkPos(x, z), fold.fold(new ChunkPos(x, z))),
                            fold.deckTransformationFrom(refPos, new ChunkPos(x, z)));
                }
            }
        }
    }
}
