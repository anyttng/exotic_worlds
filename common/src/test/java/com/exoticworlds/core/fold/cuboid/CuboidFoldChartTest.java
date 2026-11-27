package com.exoticworlds.core.fold.cuboid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.fold.DeckTransformation;
import com.exoticworlds.core.fold.SeamTransform;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.world.CoordinateConstants;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

class CuboidFoldChartTest {
    private static final double INWARD_BLOCKS = 2.0;
    private static final double OUTWARD_BLOCKS = 1.0;
    private static final double TOLERANCE = 1.0E-9;
    private static final int REACH_CHUNKS = 7;
    private static final double MIRROR_EAST_OF_THE_VERTEX_BLOCKS = 40.0;
    private static final double MIRROR_SOUTH_OF_THE_VERTEX_BLOCKS = 8.0;
    private static final CuboidAtlas BOX =
            CuboidAtlas.of(24, 16, 20, CuboidFixtures.MIN_CHUNK_X, CuboidFixtures.MIN_CHUNK_Z);

    private static Vec3 pointBy(CuboidFace face, Direction side, double outward) {
        double[] flat = CuboidFixtures.pointBy(face, side, CuboidFixtures.edgeBlocks(face, side) / 2.0, outward);
        return new Vec3(flat[0], CuboidFixtures.BLOCK_Y, flat[1]);
    }

    private static void assertStepCarriedBy(WorldFold fold, CuboidFace from, Direction side, SeamTransform crossing) {
        Vec3 inside = pointBy(from, side, -INWARD_BLOCKS);
        Vec3 outside = pointBy(from, side, OUTWARD_BLOCKS);
        DeckTransformation along = fold.foldTransformationAlong(inside, outside);
        assertNotNull(along, "a step across the " + side + " edge of " + from.side());
        double[] expected = CuboidFixtures.carried(crossing, new double[] {outside.x, outside.z});
        Vec3 carried = along.apply(outside);
        assertEquals(expected[0], carried.x, TOLERANCE, "x past the " + side + " edge of " + from.side());
        assertEquals(expected[1], carried.z, TOLERANCE, "z past the " + side + " edge of " + from.side());
    }

    @Test
    void aStepAcrossAnEdgeIsCarriedByThatEdgeOnEveryBox() {
        for (CuboidAtlas atlas : CuboidFixtures.atlases()) {
            WorldFold fold = CuboidFixtures.cuboidFold(atlas);
            for (CuboidEdge edge : atlas.edges()) {
                assertStepCarriedBy(fold, CuboidFixtures.face(atlas, edge.first()), edge.firstSide(),
                        edge.transform());
                assertStepCarriedBy(fold, CuboidFixtures.face(atlas, edge.second()), edge.secondSide(),
                        edge.transform().inverse());
            }
        }
    }

    @Test
    void aStepThatStaysOnItsFaceTakesNoMove() {
        WorldFold fold = CuboidFixtures.cuboidFold(BOX);
        CuboidFace front = CuboidFixtures.face(BOX, CuboidFace.Side.FRONT);
        Vec3 inside = pointBy(front, Direction.NORTH, -INWARD_BLOCKS);
        assertSame(DeckTransformation.IDENTITY, fold.foldTransformationAlong(inside, inside.add(1.0, 0.0, 1.0)));
    }

    @Test
    void aStepFromOffTheNetHasNoStraightPath() {
        WorldFold fold = CuboidFixtures.cuboidFold(BOX);
        CuboidFace front = CuboidFixtures.face(BOX, CuboidFace.Side.FRONT);
        Vec3 emptyCorner = new Vec3(front.minBlockX() - 8.0, CuboidFixtures.BLOCK_Y, front.minBlockZ() - 8.0);
        assertNull(fold.foldTransformationAlong(emptyCorner, emptyCorner.add(1.0, 0.0, 0.0)));
    }

    @Test
    void aChunkBesideTheVertexHasTwoStraightCopiesInReach() {
        WorldFold fold = CuboidFixtures.cuboidFold(BOX);
        CuboidFace front = CuboidFixtures.face(BOX, CuboidFace.Side.FRONT);
        ChunkPos vertexChunk = ChunkPos.containing(new BlockPos(front.minBlockX(), 0, front.minBlockZ()));
        ChunkPos ref = ChunkPos.containing(BlockPos.containing(front.minBlockX() + MIRROR_EAST_OF_THE_VERTEX_BLOCKS,
                0.0, front.minBlockZ() + MIRROR_SOUTH_OF_THE_VERTEX_BLOCKS));
        ChunkPos west = new ChunkPos(vertexChunk.x() - 1, vertexChunk.z());
        CuboidEdge topToWest = BOX.edges().stream()
                .filter(edge -> edge.first() == CuboidFace.Side.TOP && edge.firstSide() == Direction.WEST)
                .findFirst()
                .orElseThrow();
        ChunkPos throughTheTop = new DeckTransformation(topToWest.transform().inverse()).apply(west);

        List<ChunkPos> copies = fold.copiesInReach(ref, west, REACH_CHUNKS);

        assertEquals(fold.nearestCopy(ref, west), copies.getFirst());
        assertEquals(Set.of(west, throughTheTop), new HashSet<>(copies));
    }

    @Test
    void aChunkPastTheReachKeepsItsNearestCopyAlone() {
        WorldFold fold = CuboidFixtures.cuboidFold(BOX);
        CuboidFace back = CuboidFixtures.face(BOX, CuboidFace.Side.BACK);
        CuboidFace front = CuboidFixtures.face(BOX, CuboidFace.Side.FRONT);
        ChunkPos ref = ChunkPos.containing(new BlockPos(front.blockX(front.widthBlocks() / 2), 0,
                front.blockZ(front.depthBlocks() / 2)));
        ChunkPos target = ChunkPos.containing(new BlockPos(back.blockX(back.widthBlocks() / 2), 0,
                back.blockZ(back.depthBlocks() / 2)));

        assertEquals(List.of(fold.nearestCopy(ref, target)), fold.copiesInReach(ref, target, REACH_CHUNKS));
    }

    @Test
    void theChunkMoveSeatsTheNearestCopyOnEveryBox() {
        for (CuboidAtlas atlas : CuboidFixtures.atlases()) {
            WorldFold fold = CuboidFixtures.cuboidFold(atlas);
            for (CuboidFace from : atlas.faces()) {
                ChunkPos ref = ChunkPos.containing(new BlockPos(from.blockX(0), 0, from.blockZ(0)));
                for (CuboidFace to : atlas.faces()) {
                    ChunkPos target = ChunkPos.containing(new BlockPos(to.blockX(to.widthBlocks() - 1), 0,
                            to.blockZ(to.depthBlocks() - 1)));
                    assertEquals(fold.nearestCopy(ref, target), fold.nearestCopyTransformation(ref, target).apply(target),
                            from.side() + " toward " + to.side());
                }
            }
        }
    }

    @Test
    void halfTheWorldIsHalfTheSmallestEdge() {
        WorldFold fold = CuboidFixtures.cuboidFold(BOX);
        int halfEdgeChunks = 8;
        assertTrue(fold.canFitInHalfChunks(Direction.Axis.X, halfEdgeChunks));
        assertFalse(fold.canFitInHalfChunks(Direction.Axis.Z, halfEdgeChunks + 1));
        assertTrue(fold.canFitInHalfBlocks(Direction.Axis.Z,
                (double) halfEdgeChunks * CoordinateConstants.CHUNK_WIDTH_BLOCKS));
        assertFalse(fold.canFitInHalfBlocks(Direction.Axis.X,
                halfEdgeChunks * CoordinateConstants.CHUNK_WIDTH_BLOCKS + 1.0));
    }
}
