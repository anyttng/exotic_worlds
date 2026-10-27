package com.exoticworlds.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.exoticworlds.client.engine.ClientFrame;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

class ClientFrameTest {
    private static final WorldFold TORUS = WorldFolds.of(ShapeFixtures.torus(WorldLoopBounds.ofWidth(64)));
    private static final BlockPos CANONICAL = new BlockPos(500, 64, 500);
    private static final BlockPos SAME_LAP_ANCHOR = new BlockPos(480, 64, 500);
    private static final BlockPos LAP_AWAY_ANCHOR = new BlockPos(-500, 64, 500);
    private static final BlockPos HELD_COPY = new BlockPos(-524, 64, 500);
    private static final ChunkPos CANONICAL_CHUNK = new ChunkPos(31, 31);
    private static final ChunkPos SAME_LAP_ANCHOR_CHUNK = new ChunkPos(30, 31);
    private static final ChunkPos LAP_AWAY_ANCHOR_CHUNK = new ChunkPos(-32, 31);
    private static final ChunkPos HELD_COPY_CHUNK = new ChunkPos(-33, 31);
    private static final WorldFold LATTICE_TORUS =
            WorldFolds.of(FlatShape.latticeTorus(WorldLoopBounds.ofWidths(64, 128), 8));
    private static final ChunkPos ORIGIN_CHUNK = new ChunkPos(0, 0);
    private static final int FAR_CHUNK = 2_590_846;
    private static final int[] SWEEP_COORDINATES = {-200, -129, -65, -33, -32, -1, 0, 31, 32, 33, 63, 64, 97, 200,
        FAR_CHUNK};

    @Test
    void theSpellingTheClientAlreadyHoldsComesBackUnchanged() {
        assertEquals(CANONICAL, ClientFrame.heldCopy(TORUS, SAME_LAP_ANCHOR, CANONICAL, pos -> true));
    }

    @Test
    void aCopyOneLapAwayResolvesToTheSpellingTheClientHolds() {
        assertEquals(HELD_COPY, ClientFrame.heldCopy(TORUS, LAP_AWAY_ANCHOR, CANONICAL, HELD_COPY::equals));
    }

    @Test
    void aPositionTheClientDoesNotHoldAnswersNothing() {
        assertNull(ClientFrame.heldCopy(TORUS, LAP_AWAY_ANCHOR, CANONICAL, pos -> false));
    }

    @Test
    void withoutAFoldTheArgumentComesBack() {
        assertSame(CANONICAL, ClientFrame.heldCopy(null, LAP_AWAY_ANCHOR, CANONICAL, pos -> false));
    }

    @Test
    void theChunkTheClientAlreadyHoldsComesBackUnchanged() {
        assertEquals(CANONICAL_CHUNK,
                ClientFrame.heldCopy(TORUS, SAME_LAP_ANCHOR_CHUNK, CANONICAL_CHUNK, pos -> true));
    }

    @Test
    void aChunkOneLapAwayResolvesToTheSpellingTheClientHolds() {
        assertEquals(HELD_COPY_CHUNK,
                ClientFrame.heldCopy(TORUS, LAP_AWAY_ANCHOR_CHUNK, CANONICAL_CHUNK, HELD_COPY_CHUNK::equals));
    }

    @Test
    void aChunkTheClientDoesNotHoldAnswersNothing() {
        assertNull(ClientFrame.heldCopy(TORUS, LAP_AWAY_ANCHOR_CHUNK, CANONICAL_CHUNK, pos -> false));
    }

    @Test
    void withoutAFoldTheChunkArgumentComesBack() {
        assertSame(CANONICAL_CHUNK, ClientFrame.heldCopy(null, LAP_AWAY_ANCHOR_CHUNK, CANONICAL_CHUNK, pos -> false));
    }

    @Test
    void aChunkInsideTheBoundsIsItsOwnNearestCopy() {
        assertNearestKey(TORUS, ORIGIN_CHUNK, 10, 0, 10, 0);
        assertNearestKey(TORUS, ORIGIN_CHUNK, 31, 0, 31, 0);
    }

    @Test
    void theFirstChunkPastTheBoundAtHalfAWorldStaysItself() {
        assertNearestKey(TORUS, ORIGIN_CHUNK, 32, 0, 32, 0);
    }

    @Test
    void aChunkPastTheBoundFoldsToTheCopyInsideIt() {
        assertNearestKey(TORUS, ORIGIN_CHUNK, 33, 0, -31, 0);
        assertNearestKey(TORUS, ORIGIN_CHUNK, 0, 33, 0, -31);
    }

    @Test
    void aChunkAWorldWidthPastTheBoundFoldsOntoTheChunkInside() {
        assertNearestKey(TORUS, ORIGIN_CHUNK, 74, 0, 10, 0);
    }

    @Test
    void aChunkFarPastTheBoundFoldsOntoTheWorld() {
        assertNearestKey(TORUS, ORIGIN_CHUNK, FAR_CHUNK, 0, -2, 0);
    }

    @Test
    void withoutAFoldOrAnAnchorTheChunkKeyComesBack() {
        assertEquals(ChunkPos.pack(33, 0), ClientFrame.nearestChunkKey(null, ORIGIN_CHUNK, 33, 0));
        assertEquals(ChunkPos.pack(33, 0), ClientFrame.nearestChunkKey(TORUS, null, 33, 0));
    }

    @Test
    void theChunkKeyNamesTheCopyHeldCopyAsksAbout() {
        for (WorldFold fold : new WorldFold[] {TORUS, LATTICE_TORUS}) {
            for (int anchorX = -96; anchorX <= 96; anchorX += 24) {
                for (int anchorZ = -160; anchorZ <= 160; anchorZ += 40) {
                    ChunkPos anchor = new ChunkPos(anchorX, anchorZ);
                    for (int targetX : SWEEP_COORDINATES) {
                        for (int targetZ : SWEEP_COORDINATES) {
                            ChunkPos target = new ChunkPos(targetX, targetZ);
                            assertEquals(ClientFrame.heldCopy(fold, anchor, target, pos -> true),
                                    ChunkPos.unpack(ClientFrame.nearestChunkKey(fold, anchor, targetX, targetZ)),
                                    "anchor " + anchor + ", target " + target);
                        }
                    }
                }
            }
        }
    }

    @Test
    void theNearestCopyIsItsOwnNearestCopy() {
        for (WorldFold fold : new WorldFold[] {TORUS, LATTICE_TORUS}) {
            for (int anchorX = -96; anchorX <= 96; anchorX += 24) {
                for (int anchorZ = -160; anchorZ <= 160; anchorZ += 40) {
                    ChunkPos anchor = new ChunkPos(anchorX, anchorZ);
                    for (int targetX : SWEEP_COORDINATES) {
                        for (int targetZ : SWEEP_COORDINATES) {
                            long copy = ClientFrame.nearestChunkKey(fold, anchor, targetX, targetZ);
                            assertEquals(copy, ClientFrame.nearestChunkKey(fold, anchor, ChunkPos.getX(copy),
                                    ChunkPos.getZ(copy)), "anchor " + anchor + ", target " + targetX + ", " + targetZ);
                        }
                    }
                }
            }
        }
    }

    private static void assertNearestKey(WorldFold fold, ChunkPos anchor, int chunkX, int chunkZ,
            int expectedX, int expectedZ) {
        assertEquals(ChunkPos.pack(expectedX, expectedZ), ClientFrame.nearestChunkKey(fold, anchor, chunkX, chunkZ));
    }
}
