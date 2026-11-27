package com.exoticworlds.core.fold.cuboid;

import static com.exoticworlds.core.fold.cuboid.CuboidFixtures.BLOCK_Y;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.fold.DeckTransformation;
import com.exoticworlds.core.fold.FoldOrientation;
import com.exoticworlds.core.fold.SeamTransform;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.cuboid.CuboidFace.Side;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

class CuboidFoldNearestTest {
    private static final double BLOCK_CENTER = 0.5;
    private static final int FROM_VERTEX = 5;
    private static final int NEAR_VERTEX = 4;
    private static final int INWARD = 2;
    private static final int OUTWARD = 1;
    private static final int ALONG = 2;

    private record Crossing(WorldFold fold, CuboidFace from, Direction fromSide, SeamTransform transform) {
        SeamTransform back() {
            return this.transform.inverse();
        }
    }

    private interface CrossingCase {
        void accept(Crossing crossing, BlockPos inside, BlockPos outside, BlockPos target);
    }

    private static void forEveryCrossing(CrossingCase crossingCase) {
        for (CuboidAtlas atlas : CuboidFixtures.atlases()) {
            WorldFold fold = CuboidFixtures.cuboidFold(atlas);
            for (CuboidEdge edge : atlas.edges()) {
                walk(new Crossing(fold, CuboidFixtures.face(atlas, edge.first()), edge.firstSide(),
                        edge.transform()), crossingCase);
                walk(new Crossing(fold, CuboidFixtures.face(atlas, edge.second()), edge.secondSide(),
                        edge.transform().inverse()), crossingCase);
            }
        }
    }

    private static void walk(Crossing crossing, CrossingCase crossingCase) {
        for (BlockPos inside : CuboidFixtures.blocksAlong(crossing.from(), crossing.fromSide())) {
            BlockPos outside = inside.relative(crossing.fromSide());
            crossingCase.accept(crossing, inside, outside, crossing.transform().apply(outside));
        }
    }

    private static Vec3 centerOf(BlockPos block) {
        return new Vec3(block.getX() + BLOCK_CENTER, block.getY(), block.getZ() + BLOCK_CENTER);
    }

    private static double sqrDistance(WorldFold fold, BlockPos from, BlockPos to) {
        Vec3 fromCenter = centerOf(from);
        Vec3 toCenter = centerOf(to);
        return fold.sqrDistanceBlocks(fromCenter.x, fromCenter.y, fromCenter.z, toCenter.x, toCenter.y, toCenter.z);
    }

    private static SeamTransform crossingOf(CuboidAtlas atlas, Side first, Direction firstSide) {
        return atlas.edges().stream()
                .filter(edge -> edge.first() == first && edge.firstSide() == firstSide)
                .findFirst().orElseThrow().transform();
    }

    @Test
    void aTargetPastEveryEdgeIsSeatedBesideTheAnchor() {
        forEveryCrossing((crossing, inside, outside, target) -> {
            WorldFold fold = crossing.fold();

            assertEquals(outside, fold.nearestCopy(inside, target));
            assertEquals(centerOf(outside), fold.nearestCopy(centerOf(inside), centerOf(target)));
            if (crossing.transform().isIdentity()) {
                assertSame(target, fold.nearestCopy(inside, target));
            }
        });
    }

    @Test
    void theOrientedFormsAnswerTheOrientationOfTheMoveBack() {
        forEveryCrossing((crossing, inside, outside, target) -> {
            WorldFold fold = crossing.fold();
            FoldOrientation orientation = crossing.back().getOrientation();

            WorldFold.Folded<BlockPos> block = fold.nearestCopyOriented(inside, target);
            assertEquals(outside, block.value());
            assertEquals(orientation, block.orientation());

            WorldFold.Folded<Vec3> point = fold.nearestCopyOriented(centerOf(inside), centerOf(target));
            assertEquals(centerOf(outside), point.value());
            assertEquals(orientation, point.orientation());

            WorldFold.Folded<AABB> box = fold.nearestCopyOriented(centerOf(inside), new AABB(target));
            assertEquals(new AABB(outside), box.value());
            assertEquals(orientation, box.orientation());
        });
    }

    @Test
    void theTransformationCarriesTheTargetOntoItsNearestCopy() {
        forEveryCrossing((crossing, inside, outside, target) -> {
            WorldFold fold = crossing.fold();

            DeckTransformation ofBlocks = fold.nearestCopyTransformation(inside, target);
            assertEquals(fold.nearestCopy(inside, target), ofBlocks.apply(target));
            assertEquals(crossing.back().getOrientation(), ofBlocks.getOrientation());

            DeckTransformation ofPoints = fold.nearestCopyTransformation(centerOf(inside), centerOf(target));
            assertEquals(fold.nearestCopy(centerOf(inside), centerOf(target)), ofPoints.apply(centerOf(target)));
            assertEquals(ofBlocks, ofPoints);
        });
    }

    @Test
    void aChunkPastEveryEdgeIsSeatedBesideTheAnchorChunk() {
        forEveryCrossing((crossing, inside, outside, target) -> {
            WorldFold fold = crossing.fold();
            ChunkPos anchor = ChunkPos.containing(inside);
            ChunkPos chunk = ChunkPos.containing(target);
            ChunkPos seated = ChunkPos.containing(outside);

            assertEquals(seated, fold.nearestCopy(anchor, chunk));
            assertEquals(ChunkPos.pack(seated.x(), seated.z()), fold.nearestCopyChunkKey(
                    ChunkPos.pack(anchor.x(), anchor.z()), ChunkPos.pack(chunk.x(), chunk.z())));
            assertEquals(1, fold.sqrDistanceChunks(anchor, chunk));
            assertEquals(1, fold.sqrDistanceChunks(chunk, anchor));
        });
    }

    @Test
    void theDistanceAcrossAnEdgeIsTheStraightLineInTheUnfolding() {
        forEveryCrossing((crossing, inside, outside, target) -> {
            WorldFold fold = crossing.fold();

            assertEquals(1.0, sqrDistance(fold, inside, target));
            assertEquals(1.0, sqrDistance(fold, target, inside));
        });

        for (CuboidAtlas atlas : CuboidFixtures.atlases()) {
            WorldFold fold = CuboidFixtures.cuboidFold(atlas);
            for (CuboidEdge edge : atlas.edges()) {
                CuboidFace from = CuboidFixtures.face(atlas, edge.first());
                Direction side = edge.firstSide();
                List<BlockPos> along = CuboidFixtures.blocksAlong(from, side);
                BlockPos anchor = along.get(along.size() / 2).relative(side.getOpposite(), INWARD);
                BlockPos seat = anchor.relative(side, INWARD + 1 + OUTWARD).relative(side.getClockWise(), ALONG);
                BlockPos target = edge.transform().apply(seat);
                double straight = anchor.distSqr(seat);

                assertEquals(seat, fold.nearestCopy(anchor, target));
                assertEquals(straight, sqrDistance(fold, anchor, target));
                assertEquals(straight, sqrDistance(fold, target, anchor));
            }
        }
    }

    @Test
    void nearAVertexTheShorterUnfoldingIsTaken() {
        for (CuboidAtlas atlas : CuboidFixtures.atlases()) {
            WorldFold fold = CuboidFixtures.cuboidFold(atlas);
            CuboidFace front = CuboidFixtures.face(atlas, Side.FRONT);
            SeamTransform westToTop = crossingOf(atlas, Side.TOP, Direction.WEST).inverse();
            int vertexX = front.minBlockX();
            int vertexZ = front.minBlockZ();

            BlockPos topByWestEdge = new BlockPos(vertexX, BLOCK_Y, vertexZ - FROM_VERTEX);
            BlockPos westByNorthEdge = new BlockPos(vertexX - FROM_VERTEX, BLOCK_Y, vertexZ);
            BlockPos besideTop = new BlockPos(vertexX - 1, BLOCK_Y, vertexZ - FROM_VERTEX);
            assertEquals(besideTop, westToTop.apply(westByNorthEdge));
            assertEquals(besideTop, fold.nearestCopy(topByWestEdge, westByNorthEdge));
            assertEquals(westToTop.getOrientation(),
                    fold.nearestCopyOriented(topByWestEdge, westByNorthEdge).orientation());
            assertEquals(1.0, sqrDistance(fold, topByWestEdge, westByNorthEdge));
            assertEquals(1.0, sqrDistance(fold, westByNorthEdge, topByWestEdge));

            BlockPos topByFront = new BlockPos(vertexX + NEAR_VERTEX, BLOCK_Y, vertexZ - 1);
            BlockPos westByFront = new BlockPos(vertexX - 1, BLOCK_Y, vertexZ + NEAR_VERTEX);
            assertSame(westByFront, fold.nearestCopy(topByFront, westByFront));
            assertEquals(topByFront.distSqr(westByFront), sqrDistance(fold, topByFront, westByFront));

            BlockPos frontByTop = new BlockPos(vertexX + NEAR_VERTEX, BLOCK_Y, vertexZ);
            assertEquals(besideTop, fold.nearestCopy(frontByTop, westByNorthEdge));
            assertTrue(frontByTop.distSqr(besideTop) < frontByTop.distSqr(westByNorthEdge));
            assertEquals(frontByTop.distSqr(besideTop), sqrDistance(fold, frontByTop, westByNorthEdge));
            assertEquals(frontByTop.distSqr(besideTop), sqrDistance(fold, westByNorthEdge, frontByTop));
        }
    }

    @Test
    void betweenOppositeFacesTheShortestStraightUnfoldingIsTaken() {
        for (CuboidAtlas atlas : CuboidFixtures.atlases()) {
            WorldFold fold = CuboidFixtures.cuboidFold(atlas);
            CuboidFace west = CuboidFixtures.face(atlas, Side.WEST);
            CuboidFace front = CuboidFixtures.face(atlas, Side.FRONT);
            CuboidFace back = CuboidFixtures.face(atlas, Side.BACK);
            int row = front.minBlockZ() + front.depthBlocks() / 2;
            int backEnd = back.minBlockX() + back.widthBlocks();
            int aroundTheBox = backEnd - west.minBlockX();

            BlockPos frontByEast = new BlockPos(front.minBlockX() + front.widthBlocks() - 1, BLOCK_Y, row);
            BlockPos backByEast = new BlockPos(back.minBlockX(), BLOCK_Y, row);
            assertSame(backByEast, fold.nearestCopy(frontByEast, backByEast));

            BlockPos frontByWest = new BlockPos(front.minBlockX(), BLOCK_Y, row);
            BlockPos backByWest = new BlockPos(backEnd - 1, BLOCK_Y, row);
            BlockPos pastWest = backByWest.offset(-aroundTheBox, 0, 0);
            assertEquals(pastWest, fold.nearestCopy(frontByWest, backByWest));
            assertEquals(frontByWest.distSqr(pastWest), sqrDistance(fold, frontByWest, backByWest));
            assertEquals(frontByWest.distSqr(pastWest), sqrDistance(fold, backByWest, frontByWest));
        }
    }

    @Test
    void aTieBetweenOppositeFacesLeavesTheTargetAtItsPlaceInTheAtlas() {
        CuboidAtlas atlas = CuboidAtlas.of(16, 16, 16, CuboidFixtures.MIN_CHUNK_X, CuboidFixtures.MIN_CHUNK_Z);
        WorldFold fold = CuboidFixtures.cuboidFold(atlas);
        Vec3 frontCenter = centerOf(CuboidFixtures.face(atlas, Side.FRONT));
        Vec3 backCenter = centerOf(CuboidFixtures.face(atlas, Side.BACK));

        assertSame(backCenter, fold.nearestCopy(frontCenter, backCenter));
        assertSame(frontCenter, fold.nearestCopy(backCenter, frontCenter));
        assertTrue(fold.nearestCopyTransformation(frontCenter, backCenter).isIdentity());
    }

    private static Vec3 centerOf(CuboidFace face) {
        return new Vec3(face.minBlockX() + face.widthBlocks() / 2.0, BLOCK_Y,
                face.minBlockZ() + face.depthBlocks() / 2.0);
    }

    @Test
    void aPositionWithNoFaceLeavesTheTargetUnmoved() {
        for (CuboidAtlas atlas : CuboidFixtures.atlases()) {
            WorldFold fold = CuboidFixtures.cuboidFold(atlas);
            CuboidFace top = CuboidFixtures.face(atlas, Side.TOP);
            CuboidFace back = CuboidFixtures.face(atlas, Side.BACK);
            BlockPos unseated = new BlockPos(
                    top.minBlockX() - 1, BLOCK_Y, top.minBlockZ() + top.depthBlocks() - 1);
            BlockPos seated = new BlockPos(back.minBlockX(), BLOCK_Y, back.minBlockZ());

            assertTrue(fold.isOver(unseated));
            assertSame(unseated, fold.fold(unseated));
            assertSame(unseated, fold.nearestCopy(seated, unseated));
            assertSame(seated, fold.nearestCopy(unseated, seated));
            assertTrue(fold.nearestCopyTransformation(unseated, seated).isIdentity());
            assertEquals(unseated.distSqr(seated), sqrDistance(fold, unseated, seated));
        }
    }

    @Test
    void anAnchorPastAnEdgeReadsItsOwnFrame() {
        forEveryCrossing((crossing, inside, outside, target) -> {
            WorldFold fold = crossing.fold();
            BlockPos further = outside.relative(crossing.fromSide());

            assertSame(inside, fold.nearestCopy(outside, inside));
            assertSame(further, fold.nearestCopy(outside, further));
            assertEquals(1.0, sqrDistance(fold, outside, inside));
        });
    }
}
