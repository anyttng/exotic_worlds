package com.exoticworlds.core.fold.cuboid;

import static com.exoticworlds.core.fold.cuboid.CuboidFixtures.BLOCK_Y;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.fold.DeckTransformation;
import com.exoticworlds.core.fold.SeamTransform;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.cuboid.CuboidFace.Side;
import com.exoticworlds.core.fold.WorldFold.Folded;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

class CuboidFoldRegionsTest {
    private static final double BLOCK_CENTER = 0.5;
    private static final int INWARD = 1;
    private static final int OUTWARD = 2;
    private static final int HEIGHT = 1;
    private static final int FAR_ABOVE = 10;
    private static final int AROUND_VERTEX = 2;
    private static final int VERTEX_PIECES = 5;

    private record Crossing(CuboidAtlas atlas, WorldFold fold, CuboidFace from, Direction fromSide,
            SeamTransform transform) {
        private List<BlockPos> middle() {
            List<BlockPos> blocks = CuboidFixtures.blocksAlong(this.from, this.fromSide);
            int middle = blocks.size() / 2;
            return List.of(blocks.get(middle), blocks.get(middle + 1));
        }

        BoundingBox inside() {
            List<BlockPos> middle = middle();
            return BoundingBox.fromCorners(middle.get(0).relative(this.fromSide.getOpposite(), INWARD),
                    middle.get(1).above(HEIGHT));
        }

        BoundingBox outside() {
            List<BlockPos> middle = middle();
            return BoundingBox.fromCorners(middle.get(0).relative(this.fromSide),
                    middle.get(1).relative(this.fromSide, OUTWARD).above(HEIGHT));
        }

        BoundingBox across() {
            return BoundingBox.encapsulating(inside(), outside());
        }
    }

    private interface CrossingCase {
        void accept(Crossing crossing);
    }

    private static void forEveryCrossing(CrossingCase crossingCase) {
        for (CuboidAtlas atlas : CuboidFixtures.atlases()) {
            WorldFold fold = CuboidFixtures.cuboidFold(atlas);
            for (CuboidEdge edge : atlas.edges()) {
                crossingCase.accept(new Crossing(atlas, fold, CuboidFixtures.face(atlas, edge.first()),
                        edge.firstSide(), edge.transform()));
                crossingCase.accept(new Crossing(atlas, fold, CuboidFixtures.face(atlas, edge.second()),
                        edge.secondSide(), edge.transform().inverse()));
            }
        }
    }

    private static Set<BlockPos> blocksOf(BoundingBox region) {
        Set<BlockPos> blocks = new HashSet<>();
        for (int x = region.minX(); x <= region.maxX(); x++) {
            for (int y = region.minY(); y <= region.maxY(); y++) {
                for (int z = region.minZ(); z <= region.maxZ(); z++) {
                    blocks.add(new BlockPos(x, y, z));
                }
            }
        }

        return blocks;
    }

    private static Set<BlockPos> blocksOf(List<Folded<BoundingBox>> pieces) {
        Set<BlockPos> blocks = new HashSet<>();
        for (Folded<BoundingBox> piece : pieces) {
            blocks.addAll(blocksOf(piece.value()));
        }

        return blocks;
    }

    private static int blockCount(List<Folded<BoundingBox>> pieces) {
        int count = 0;
        for (Folded<BoundingBox> piece : pieces) {
            count += blocksOf(piece.value()).size();
        }

        return count;
    }

    private static AABB boxOf(BoundingBox region) {
        return new AABB(region.minX(), region.minY(), region.minZ(),
                region.maxX() + 1, region.maxY() + 1, region.maxZ() + 1);
    }

    private static Vec3 centerOf(BlockPos block) {
        return new Vec3(block.getX() + BLOCK_CENTER, block.getY() + BLOCK_CENTER, block.getZ() + BLOCK_CENTER);
    }

    private static SeamTransform crossingOf(CuboidAtlas atlas, Side first, Direction firstSide) {
        return atlas.edges().stream()
                .filter(edge -> edge.first() == first && edge.firstSide() == firstSide)
                .findFirst().orElseThrow().transform();
    }

    private static BoundingBox aroundTopWestVertex(CuboidAtlas atlas) {
        CuboidFace front = CuboidFixtures.face(atlas, Side.FRONT);
        return new BoundingBox(
                front.minBlockX() - AROUND_VERTEX, BLOCK_Y, front.minBlockZ() - AROUND_VERTEX,
                front.minBlockX() + AROUND_VERTEX - 1, BLOCK_Y, front.minBlockZ() + AROUND_VERTEX - 1);
    }

    @Test
    void aRegionAcrossEveryEdgeSplitsIntoTheBlocksItsBlocksFoldTo() {
        forEveryCrossing(crossing -> {
            WorldFold fold = crossing.fold();
            BoundingBox region = crossing.across();
            Set<BlockPos> folded = new HashSet<>();
            for (BlockPos block : blocksOf(region)) {
                folded.add(fold.fold(block));
            }

            List<Folded<BoundingBox>> pieces = fold.split(region);

            assertTrue(fold.isCrossingBounds(region));
            assertEquals(2, pieces.size());
            assertEquals(folded, blocksOf(pieces));
            assertEquals(blocksOf(region).size(), blockCount(pieces));
        });
    }

    @Test
    void everyPieceCarriesTheOrientationItsBlocksFoldWith() {
        forEveryCrossing(crossing -> {
            WorldFold fold = crossing.fold();
            BoundingBox region = crossing.across();
            List<Folded<BoundingBox>> pieces = fold.split(region);
            List<Folded<AABB>> boxes = fold.split(boxOf(region));

            assertEquals(2, boxes.size());
            for (BlockPos block : blocksOf(region)) {
                Folded<BlockPos> seated = fold.foldOriented(block);
                Folded<BoundingBox> piece = pieces.stream()
                        .filter(candidate -> candidate.value().isInside(seated.value()))
                        .findFirst().orElseThrow();
                Folded<Vec3> point = fold.foldOriented(centerOf(block));
                Folded<AABB> box = boxes.stream()
                        .filter(candidate -> candidate.value().contains(point.value()))
                        .findFirst().orElseThrow();

                assertEquals(seated.orientation(), piece.orientation());
                assertEquals(point.orientation(), box.orientation());
            }
        });
    }

    @Test
    void aRegionInsideOneFaceComesBackAsItself() {
        forEveryCrossing(crossing -> {
            WorldFold fold = crossing.fold();
            BoundingBox region = crossing.inside();
            AABB box = boxOf(region);

            assertFalse(fold.isCrossingBounds(region));
            assertFalse(fold.isCrossingBounds(box));
            assertEquals(1, fold.split(region).size());
            assertSame(region, fold.split(region).get(0).value());
            assertTrue(fold.split(region).get(0).isIdentity());
            assertEquals(1, fold.split(box).size());
            assertSame(box, fold.split(box).get(0).value());
        });
    }

    @Test
    void twoRegionsThatMeetOnlyThroughAnEdgeOverlap() {
        forEveryCrossing(crossing -> {
            WorldFold fold = crossing.fold();
            BoundingBox landed = crossing.transform().apply(crossing.outside());

            assertTrue(fold.areRegionsOverlapping(crossing.across(), landed));
            assertTrue(fold.areRegionsOverlapping(landed, crossing.across()));
            assertTrue(fold.areBoxesOverlapping(boxOf(crossing.across()), boxOf(landed)));
            assertFalse(fold.areRegionsOverlapping(crossing.inside(), landed));
            assertFalse(fold.areRegionsOverlapping(crossing.across(), landed.moved(0, FAR_ABOVE, 0)));
        });
    }

    @Test
    void aRegionSmallerThanAnEdgeDoesNotFoldOntoItself() {
        forEveryCrossing(crossing -> {
            assertFalse(crossing.fold().isFoldedOntoItself(crossing.across()));
            assertFalse(crossing.fold().isFoldedOntoItself(crossing.inside()));
        });
    }

    @Test
    void aRegionAroundTheBoxFoldsOntoItself() {
        for (CuboidAtlas atlas : CuboidFixtures.atlases()) {
            WorldFold fold = CuboidFixtures.cuboidFold(atlas);
            CuboidFace west = CuboidFixtures.face(atlas, Side.WEST);
            CuboidFace back = CuboidFixtures.face(atlas, Side.BACK);
            int pastBack = back.minBlockX() + back.widthBlocks();

            assertTrue(fold.isFoldedOntoItself(new BoundingBox(
                    west.minBlockX(), BLOCK_Y, west.minBlockZ(), pastBack, BLOCK_Y, west.minBlockZ())));
            assertFalse(fold.isFoldedOntoItself(new BoundingBox(
                    west.minBlockX(), BLOCK_Y, west.minBlockZ(), pastBack - 1, BLOCK_Y, west.minBlockZ())));
        }
    }

    @Test
    void theEmptyCornerAtAVertexIsCarriedWholeIntoBothFaces() {
        for (CuboidAtlas atlas : CuboidFixtures.atlases()) {
            WorldFold fold = CuboidFixtures.cuboidFold(atlas);
            CuboidFace front = CuboidFixtures.face(atlas, Side.FRONT);
            SeamTransform topToWest = crossingOf(atlas, Side.TOP, Direction.WEST);
            BoundingBox region = aroundTopWestVertex(atlas);
            BoundingBox corner = new BoundingBox(region.minX(), BLOCK_Y, region.minZ(),
                    front.minBlockX() - 1, BLOCK_Y, front.minBlockZ() - 1);

            List<Folded<BoundingBox>> pieces = fold.split(region);
            Set<BlockPos> covered = blocksOf(pieces);

            assertEquals(VERTEX_PIECES, pieces.size());
            assertTrue(pieces.contains(new Folded<>(topToWest.apply(corner), topToWest.getOrientation())));
            assertTrue(pieces.contains(
                    new Folded<>(topToWest.inverse().apply(corner), topToWest.inverse().getOrientation())));
            for (BlockPos block : blocksOf(region)) {
                BlockPos seated = fold.fold(block);
                assertTrue(fold.isOver(seated) || covered.contains(seated));
            }

            assertTrue(fold.isFoldedOntoItself(region));
        }
    }

    @Test
    void theCopiesTouchingARegionAreTheMovesBackFromItsPieces() {
        forEveryCrossing(crossing -> {
            WorldFold fold = crossing.fold();
            BoundingBox region = crossing.across();
            List<DeckTransformation> copies = fold.copiesTouching(region, 1);

            assertSame(DeckTransformation.IDENTITY, copies.get(0));
            assertEquals(List.of(DeckTransformation.IDENTITY), fold.copiesTouching(region, 0));
            assertEquals(copies, fold.copiesTouching(region));
            assertThrows(IllegalArgumentException.class, () -> fold.copiesTouching(region, -1));
            if (crossing.transform().isIdentity()) {
                assertEquals(1, copies.size());
                return;
            }

            assertEquals(2, copies.size());
            assertEquals(crossing.transform().inverse(), copies.get(1).blockTransform());
            for (BlockPos block : blocksOf(crossing.outside())) {
                assertEquals(block, copies.get(1).apply(fold.fold(block)));
            }
        });
    }

    @Test
    void aVertexListsACopyForEachPathAroundIt() {
        for (CuboidAtlas atlas : CuboidFixtures.atlases()) {
            WorldFold fold = CuboidFixtures.cuboidFold(atlas);
            SeamTransform topToWest = crossingOf(atlas, Side.TOP, Direction.WEST);

            List<DeckTransformation> copies = fold.copiesTouching(aroundTopWestVertex(atlas));

            assertEquals(List.of(DeckTransformation.IDENTITY, new DeckTransformation(topToWest),
                    new DeckTransformation(topToWest.inverse())), copies);
        }
    }
}
