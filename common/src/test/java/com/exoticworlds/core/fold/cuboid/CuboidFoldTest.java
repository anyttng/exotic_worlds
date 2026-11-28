package com.exoticworlds.core.fold.cuboid;

import static com.exoticworlds.core.fold.cuboid.CuboidFixtures.BLOCK_Y;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.world.CoordinateConstants;
import com.exoticworlds.core.fold.DeckTransformation;
import com.exoticworlds.core.fold.FoldOrientation;
import com.exoticworlds.core.fold.SeamTransform;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.fold.cuboid.CuboidFace.Side;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

class CuboidFoldTest {
    private static final int CHUNK = CoordinateConstants.CHUNK_WIDTH_BLOCKS;
    private static final int SECTION_Y = 4;
    private static final double BLOCK_CENTER = 0.5;

    private record Crossing(CuboidAtlas atlas, WorldFold fold, CuboidFace from, Direction fromSide, CuboidFace to,
            Direction toSide, SeamTransform transform) {
    }

    private interface CrossingCase {
        void accept(Crossing crossing, BlockPos inside, BlockPos outside);
    }

    private static void forEveryCrossing(CrossingCase crossingCase) {
        for (CuboidAtlas atlas : CuboidFixtures.atlases()) {
            WorldFold fold = CuboidFixtures.cuboidFold(atlas);
            for (CuboidEdge edge : atlas.edges()) {
                CuboidFace first = CuboidFixtures.face(atlas, edge.first());
                CuboidFace second = CuboidFixtures.face(atlas, edge.second());
                walk(new Crossing(atlas, fold, first, edge.firstSide(), second, edge.secondSide(),
                        edge.transform()), crossingCase);
                walk(new Crossing(atlas, fold, second, edge.secondSide(), first, edge.firstSide(),
                        edge.transform().inverse()), crossingCase);
            }
        }
    }

    private static void walk(Crossing crossing, CrossingCase crossingCase) {
        for (BlockPos inside : CuboidFixtures.blocksAlong(crossing.from(), crossing.fromSide())) {
            crossingCase.accept(crossing, inside, inside.relative(crossing.fromSide()));
        }
    }

    private static boolean blockTied(Crossing crossing, BlockPos outside) {
        return tied(crossing, outside, 1);
    }

    private static boolean chunkTied(Crossing crossing, ChunkPos chunk) {
        return tied(crossing, new BlockPos(chunk.getMinBlockX(), BLOCK_Y, chunk.getMinBlockZ()), CHUNK);
    }

    private static boolean tied(Crossing crossing, BlockPos cell, int width) {
        CuboidAtlas atlas = crossing.atlas();
        if (atlas.faceAt(cell.getX(), cell.getZ()) != null) {
            return false;
        }

        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos beside = cell.relative(side, width);
            if (atlas.faceAt(beside.getX(), beside.getZ()) == crossing.to()) {
                return true;
            }
        }

        return false;
    }

    private static Vec3 centerOf(BlockPos block) {
        return new Vec3(block.getX() + BLOCK_CENTER, block.getY(), block.getZ() + BLOCK_CENTER);
    }

    @Test
    void aBlockPastEveryEdgeLandsOnTheNeighboringFaceAtTheSameBoxPoint() {
        forEveryCrossing((crossing, inside, outside) -> {
            WorldFold fold = crossing.fold();
            assertSame(inside, fold.fold(inside));
            if (blockTied(crossing, outside)) {
                assertSame(outside, fold.fold(outside));
                assertTrue(fold.isOver(outside));
                return;
            }

            BlockPos landed = fold.fold(outside);
            assertFalse(fold.isOver(landed));
            assertEquals(!crossing.transform().isIdentity(), fold.isOver(outside));
            assertSame(crossing.to(), crossing.atlas().faceAt(landed.getX(), landed.getZ()));
            assertEquals(CuboidFixtures.sharedSegment(crossing.from(), inside, crossing.fromSide()),
                    CuboidFixtures.sharedSegment(crossing.to(), landed, crossing.toSide()));
            assertEquals(landed.asLong(), fold.foldBlockNode(outside.asLong()));
            assertEquals(crossing.transform().apply(centerOf(outside)), fold.fold(centerOf(outside)));
            assertEquals(fold.isOver(outside), fold.isOver(centerOf(outside)));
        });
    }

    @Test
    void theOrientedFoldAnswersTheOrientationOfTheCrossing() {
        forEveryCrossing((crossing, inside, outside) -> {
            WorldFold fold = crossing.fold();
            FoldOrientation orientation = blockTied(crossing, outside)
                    ? FoldOrientation.IDENTITY
                    : crossing.transform().getOrientation();

            WorldFold.Folded<BlockPos> block = fold.foldOriented(outside);
            assertEquals(fold.fold(outside), block.value());
            assertEquals(orientation, block.orientation());

            WorldFold.Folded<Vec3> point = fold.foldOriented(centerOf(outside));
            assertEquals(fold.fold(centerOf(outside)), point.value());
            assertEquals(orientation, point.orientation());

            ChunkPos chunk = ChunkPos.containing(outside);
            assertEquals(chunkTied(crossing, chunk) ? FoldOrientation.IDENTITY : crossing.transform().getOrientation(),
                    fold.foldOriented(chunk).orientation());
            assertEquals(orientation, fold.foldTransformation(centerOf(outside)).getOrientation());
            assertEquals(fold.fold(outside), fold.foldTransformation(centerOf(outside)).apply(outside));
        });
    }

    @Test
    void fiveEdgesKeepTheFrameFourTurnAQuarterTwoAHalfAndOneOnlyShifts() {
        for (CuboidAtlas atlas : CuboidFixtures.atlases()) {
            Map<FoldOrientation, Integer> edges = new EnumMap<>(FoldOrientation.class);
            int shifted = 0;
            for (CuboidEdge edge : atlas.edges()) {
                edges.merge(edge.transform().getOrientation(), 1, Integer::sum);
                shifted += edge.transform().getOrientation().isIdentity() && !edge.transform().isIdentity() ? 1 : 0;
            }

            assertEquals(6, edges.get(FoldOrientation.IDENTITY));
            assertEquals(1, shifted);
            assertEquals(2, edges.get(FoldOrientation.HALF_TURN));
            assertEquals(4, edges.get(FoldOrientation.QUARTER_TURN_CLOCKWISE)
                    + edges.get(FoldOrientation.QUARTER_TURN_COUNTERCLOCKWISE));
        }
    }

    @Test
    void aChunkFoldsAsItsBlocksDo() {
        forEveryCrossing((crossing, inside, outside) -> {
            WorldFold fold = crossing.fold();
            ChunkPos chunk = ChunkPos.containing(outside);
            if (chunkTied(crossing, chunk)) {
                assertSame(chunk, fold.fold(chunk));
                return;
            }

            WorldFold.Folded<ChunkPos> folded = fold.foldOriented(chunk);
            int[] table = folded.orientation().localIndexTable(CHUNK);

            assertEquals(folded.value(), fold.fold(chunk));
            for (int localZ = 0; localZ < CHUNK; localZ++) {
                for (int localX = 0; localX < CHUNK; localX++) {
                    BlockPos block = fold.fold(
                            new BlockPos(chunk.getMinBlockX() + localX, BLOCK_Y, chunk.getMinBlockZ() + localZ));
                    assertEquals(folded.value(), ChunkPos.containing(block));
                    assertEquals(table[localZ * CHUNK + localX],
                            (block.getZ() - folded.value().getMinBlockZ()) * CHUNK
                                    + block.getX() - folded.value().getMinBlockX());
                }
            }
        });
    }

    @Test
    void everyChunkFormAnswersTheSameChunk() {
        forEveryCrossing((crossing, inside, outside) -> {
            WorldFold fold = crossing.fold();
            ChunkPos chunk = ChunkPos.containing(outside);
            ChunkPos folded = fold.fold(chunk);

            assertEquals(ChunkPos.pack(folded.x(), folded.z()), fold.foldChunkKey(ChunkPos.pack(chunk.x(), chunk.z())));
            assertEquals(SectionPos.of(folded.x(), SECTION_Y, folded.z()),
                    fold.fold(SectionPos.of(chunk.x(), SECTION_Y, chunk.z())));
            assertEquals(SectionPos.asLong(folded.x(), SECTION_Y, folded.z()),
                    fold.foldSectionNode(SectionPos.asLong(chunk.x(), SECTION_Y, chunk.z())));
            assertEquals(fold.isOver(outside), fold.isOver(chunk));
            assertEquals(fold.isOver(chunk) ? 1 : 0, fold.chunkOvershoot(chunk));
        });
    }

    @Test
    void aCopyIsReachedByTheMoveBackFromItsChunk() {
        forEveryCrossing((crossing, inside, outside) -> {
            WorldFold fold = crossing.fold();
            ChunkPos copy = ChunkPos.containing(outside);
            if (chunkTied(crossing, copy)) {
                return;
            }

            ChunkPos chunk = fold.fold(copy);
            DeckTransformation toCopy = fold.deckTransformation(chunk, copy);

            assertEquals(copy, toCopy.apply(chunk));
            assertEquals(crossing.transform().getOrientation().inverse(), toCopy.getOrientation());
            BlockPos folded = fold.fold(outside);
            assertEquals(outside, fold.deckTransformation(ChunkPos.containing(folded), copy).apply(folded));
            assertSame(DeckTransformation.IDENTITY, fold.deckTransformation(chunk, chunk));
        });
    }

    @Test
    void aChunkThatIsNoCopyIsRefused() {
        for (CuboidAtlas atlas : CuboidFixtures.atlases()) {
            WorldFold fold = CuboidFixtures.cuboidFold(atlas);
            ChunkPos front = chunkOf(CuboidFixtures.face(atlas, Side.FRONT));
            ChunkPos back = chunkOf(CuboidFixtures.face(atlas, Side.BACK));

            assertThrows(IllegalArgumentException.class, () -> fold.deckTransformation(front, back));
        }
    }

    private static ChunkPos chunkOf(CuboidFace face) {
        return ChunkPos.containing(new BlockPos(face.minBlockX(), BLOCK_Y, face.minBlockZ()));
    }

    @Test
    void aWalkPastTwoEdgesComposesTheirCrossings() {
        for (CuboidAtlas atlas : CuboidFixtures.atlases()) {
            WorldFold fold = CuboidFixtures.cuboidFold(atlas);
            CuboidFace top = CuboidFixtures.face(atlas, Side.TOP);
            CuboidFace back = CuboidFixtures.face(atlas, Side.BACK);
            SeamTransform topToBack = atlas.edges().stream()
                    .filter(edge -> edge.first() == Side.TOP && edge.firstSide() == Direction.NORTH)
                    .findFirst().orElseThrow().transform();
            BlockPos far = new BlockPos(top.minBlockX(), BLOCK_Y, top.minBlockZ() - back.depthBlocks() - 1);
            BlockPos pastBack = topToBack.apply(far);

            assertTrue(fold.isOver(pastBack));
            assertEquals(fold.fold(pastBack), fold.fold(far));
            assertSame(CuboidFixtures.face(atlas, Side.BOTTOM),
                    atlas.faceAt(fold.fold(far).getX(), fold.fold(far).getZ()));
            assertEquals(FoldOrientation.IDENTITY, fold.foldOriented(far).orientation());
        }
    }

    @Test
    void aPositionWithNoAskingFaceStaysWhereItIs() {
        for (CuboidAtlas atlas : CuboidFixtures.atlases()) {
            WorldFold fold = CuboidFixtures.cuboidFold(atlas);
            CuboidFace west = CuboidFixtures.face(atlas, Side.WEST);
            CuboidFace top = CuboidFixtures.face(atlas, Side.TOP);
            CuboidFace bottom = CuboidFixtures.face(atlas, Side.BOTTOM);
            int topEast = top.minBlockX() + top.widthBlocks();
            int topSouth = top.minBlockZ() + top.depthBlocks() - 1;
            List<BlockPos> unseated = List.of(
                    new BlockPos(top.minBlockX() - 1, BLOCK_Y, topSouth),
                    new BlockPos(topEast, BLOCK_Y, topSouth),
                    new BlockPos(bottom.minBlockX() - 1, BLOCK_Y, bottom.minBlockZ()),
                    new BlockPos(topEast, BLOCK_Y, bottom.minBlockZ()),
                    new BlockPos(west.minBlockX() - 1, BLOCK_Y, top.minBlockZ() - 1));
            for (BlockPos block : unseated) {
                ChunkPos chunk = ChunkPos.containing(block);

                assertTrue(fold.isOver(block));
                assertSame(block, fold.fold(block));
                assertSame(chunk, fold.fold(chunk));
                assertTrue(fold.foldOriented(block).isIdentity());
                assertTrue(fold.foldTransformation(centerOf(block)).isIdentity());
                assertTrue(fold.chunkOvershoot(chunk) > 0);
            }
        }
    }

    @Test
    void theViewDistanceIsHalfTheSmallestEdgeLessTheMargin() {
        assertEquals(5, fold(16, 16, 16).getMaxViewDistanceChunks());
        assertEquals(5, fold(16, 24, 40).getMaxViewDistanceChunks());
        assertEquals(9, fold(40, 24, 32).getMaxViewDistanceChunks());
        assertEquals(1, fold(1, 1, 1).getMaxViewDistanceChunks());
        assertEquals(5, fold(16, 16, 16).limitViewDistance(12));
    }

    private static WorldFold fold(int aChunks, int bChunks, int cChunks) {
        return CuboidFixtures.cuboidFold(CuboidAtlas.of(aChunks, bChunks, cChunks, 0, 0));
    }

    @Test
    void theFoldCarriesItsAtlasAndOptionsAndNoPeriod() {
        CuboidAtlas atlas = CuboidAtlas.of(16, 16, 16, 0, 0);
        WorldFold fold = new CuboidFold(atlas, ShapeOptions.DEFAULT);

        assertSame(atlas, fold.getCuboidAtlas());
        assertSame(ShapeOptions.DEFAULT, fold.getOptions());
        assertTrue(fold.isWrapped());
        assertFalse(fold.isDecomposedPerAxis());
        assertFalse(fold.isPreservingLocalIndices());
        assertThrows(IllegalStateException.class, fold::getBounds);
        assertThrows(IllegalStateException.class, fold::getBlockLattice);
        assertThrows(IllegalStateException.class, fold::getChunkLattice);
        assertThrows(IllegalStateException.class, () -> fold.getBlockDomain(Direction.Axis.X));
        assertThrows(IllegalStateException.class, () -> fold.getChunkDomain(Direction.Axis.Z));
    }
}
