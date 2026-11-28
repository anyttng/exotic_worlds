package com.exoticworlds.engine.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.fold.DeckTransformation;
import com.exoticworlds.core.fold.cuboid.CuboidFixtures;
import com.exoticworlds.core.world.CuboidShape;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.core.world.WorldLoopBounds;
import com.exoticworlds.engine.fold.ChunkSeat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

class RegionReachTest {
    private static final WorldFold TORUS = WorldFolds.of(ShapeFixtures.torus(WorldLoopBounds.ofWidth(64)));
    private static final ChunkPos CENTER = new ChunkPos(31, 0);
    private static final int REACH_CHUNKS = 2;

    private static final WorldFold CUBE_32 = CuboidFixtures.cuboidFold(new CuboidShape(32, 32, 32).atlas());
    private static final List<ChunkPos> CENTERS_BESIDE_A_VERTEX = List.of(new ChunkPos(-17, -16), new ChunkPos(-16, -17));
    private static final ChunkPos PAST_THE_VERTEX = new ChunkPos(-17, -17);
    private static final int REGION_RADIUS_CHUNKS = 8;
    private static final int BLOCK_Y = 64;
    private static final int LAST_LOCAL_BLOCK = 15;
    private static final int MIDDLE_LOCAL_BLOCK = 7;

    private static ChunkPos answeredChunk(WorldFold fold, ChunkPos center, int chunkX, int chunkZ) {
        ChunkPos nearest = RegionReach.nearestPastTheReach(fold, center, REACH_CHUNKS, chunkX, chunkZ);
        return fold.foldFrom(center, nearest == null ? new ChunkPos(chunkX, chunkZ) : nearest);
    }

    private static List<BlockPos> blocksOf(ChunkPos chunk) {
        int minX = chunk.getMinBlockX();
        int minZ = chunk.getMinBlockZ();
        return List.of(
                new BlockPos(minX, BLOCK_Y, minZ),
                new BlockPos(minX + LAST_LOCAL_BLOCK, BLOCK_Y, minZ),
                new BlockPos(minX, BLOCK_Y, minZ + LAST_LOCAL_BLOCK),
                new BlockPos(minX + LAST_LOCAL_BLOCK, BLOCK_Y, minZ + LAST_LOCAL_BLOCK),
                new BlockPos(minX + MIDDLE_LOCAL_BLOCK, BLOCK_Y, minZ + MIDDLE_LOCAL_BLOCK));
    }

    @Test
    void aChunkInsideTheStepsReachKeepsItsOwnCoordinates() {
        assertNull(RegionReach.nearestPastTheReach(TORUS, CENTER, REACH_CHUNKS, 32, 0));
    }

    @Test
    void aChunkPastTheReachWithoutANearerCopyKeepsItsOwnCoordinates() {
        assertNull(RegionReach.nearestPastTheReach(TORUS, CENTER, REACH_CHUNKS, 28, 0));
    }

    @Test
    void aChunkPastTheReachTakesItsCopyNearestTheGeneratingChunk() {
        assertEquals(new ChunkPos(33, 0), RegionReach.nearestPastTheReach(TORUS, CENTER, REACH_CHUNKS, -31, 0));
    }

    @Test
    void anUnwrappedLevelNeverMovesAChunk() {
        assertNull(RegionReach.nearestPastTheReach(null, CENTER, REACH_CHUNKS, -31, 0));
        assertSame(DeckTransformation.IDENTITY,
                RegionReach.moveToAnsweredChunk(null, CENTER, REACH_CHUNKS, -31, 0));
    }

    @Test
    void everySlotOfARegionBesideAVertexIsCarriedOntoTheChunkTheRegionAnswers() {
        for (ChunkPos center : CENTERS_BESIDE_A_VERTEX) {
            for (int x = center.x() - REGION_RADIUS_CHUNKS; x <= center.x() + REGION_RADIUS_CHUNKS; x++) {
                for (int z = center.z() - REGION_RADIUS_CHUNKS; z <= center.z() + REGION_RADIUS_CHUNKS; z++) {
                    ChunkPos answered = answeredChunk(CUBE_32, center, x, z);
                    DeckTransformation move = RegionReach.moveToAnsweredChunk(CUBE_32, center, REACH_CHUNKS, x, z);
                    String where = "slot " + x + "," + z + " of " + center;

                    assertEquals(answered, move.apply(new ChunkPos(x, z)), where);
                    for (BlockPos block : blocksOf(new ChunkPos(x, z))) {
                        BlockPos seated = move.apply(block);

                        assertTrue(ChunkSeat.isSittingIn(answered, seated.getX(), seated.getZ()),
                                block + " of " + where + " sat at " + seated);
                    }
                }
            }
        }
    }

    @Test
    void theSlotPastTheVertexIsCarriedWhereTheAbsoluteFoldMisses() {
        ChunkPos center = CENTERS_BESIDE_A_VERTEX.getFirst();
        DeckTransformation move = RegionReach.moveToAnsweredChunk(CUBE_32, center, REACH_CHUNKS,
                PAST_THE_VERTEX.x(), PAST_THE_VERTEX.z());

        assertEquals(new ChunkPos(-16, -17), move.apply(PAST_THE_VERTEX));
        assertNotEquals(move.apply(PAST_THE_VERTEX), CUBE_32.fold(PAST_THE_VERTEX));
    }

    @Test
    void aTorusSlotIsCarriedByTheDeckTransformationOntoTheChunkItHolds() {
        for (int x = CENTER.x() - REGION_RADIUS_CHUNKS; x <= CENTER.x() + REGION_RADIUS_CHUNKS; x++) {
            for (int z = CENTER.z() - REGION_RADIUS_CHUNKS; z <= CENTER.z() + REGION_RADIUS_CHUNKS; z++) {
                ChunkPos answered = answeredChunk(TORUS, CENTER, x, z);
                DeckTransformation move = RegionReach.moveToAnsweredChunk(TORUS, CENTER, REACH_CHUNKS, x, z);
                for (BlockPos block : blocksOf(new ChunkPos(x, z))) {
                    assertEquals(TORUS.deckTransformation(ChunkPos.containing(block), answered).apply(block),
                            move.apply(block), block + " of slot " + x + "," + z);
                }
            }
        }
    }
}
