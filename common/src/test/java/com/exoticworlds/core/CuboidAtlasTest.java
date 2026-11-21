package com.exoticworlds.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.CuboidFace.BoxVector;
import com.exoticworlds.core.CuboidFace.Side;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

class CuboidAtlasTest {
    private static final int[][] BOXES = {{16, 16, 16, 0, 0}, {16, 24, 40, -37, 11}, {30, 17, 52, 5, -90}};
    private static final int BOX_EDGES = 12;
    private static final int GLUED_PAIRS = 7;
    private static final int VERTICES = 8;
    private static final int FACES_PER_VERTEX = 3;

    private static List<CuboidAtlas> atlases() {
        List<CuboidAtlas> atlases = new ArrayList<>();
        for (int[] box : BOXES) {
            atlases.add(CuboidAtlas.of(box[0], box[1], box[2], box[3], box[4]));
        }

        return atlases;
    }

    private static CuboidFace faceOf(CuboidAtlas atlas, Side side) {
        return atlas.faces().stream().filter(face -> face.side() == side).findFirst().orElseThrow();
    }

    @Test
    void everyBlockOfTheNetLiesOnOneFaceAndMapsBack() {
        for (int[] box : BOXES) {
            CuboidAtlas atlas = CuboidAtlas.of(box[0], box[1], box[2], box[3], box[4]);
            long a = (long) box[0] * CoordinateConstants.CHUNK_WIDTH;
            long b = (long) box[1] * CoordinateConstants.CHUNK_WIDTH;
            long c = (long) box[2] * CoordinateConstants.CHUNK_WIDTH;
            int minX = box[3] * CoordinateConstants.CHUNK_WIDTH;
            int minZ = box[4] * CoordinateConstants.CHUNK_WIDTH;
            long onFaces = 0;
            for (int x = minX; x < minX + 2 * (a + c); x++) {
                for (int z = minZ; z < minZ + b + 2 * c; z++) {
                    onFaces += facesHolding(atlas, x, z);
                }
            }

            assertEquals(2 * (a * b + a * c + b * c), onFaces);
        }
    }

    private static int facesHolding(CuboidAtlas atlas, int x, int z) {
        int blockX = x;
        int blockZ = z;
        long holding = atlas.faces().stream().filter(face -> face.contains(blockX, blockZ)).count();
        CuboidFace face = atlas.faceAt(x, z);
        if (holding == 0) {
            assertNull(face);
            return 0;
        }

        assertEquals(1, holding);
        assertEquals(x, face.blockX(face.localX(x)));
        assertEquals(z, face.blockZ(face.localZ(z)));
        return 1;
    }

    @Test
    void crossingEveryEdgeLandsOnTheBlockTheBoxNames() {
        for (CuboidAtlas atlas : atlases()) {
            assertEquals(BOX_EDGES, atlas.edges().size());
            assertEquals(GLUED_PAIRS,
                    atlas.edges().stream().filter(edge -> !edge.transform().isIdentity()).count());
            for (CuboidEdge edge : atlas.edges()) {
                CuboidFace first = faceOf(atlas, edge.first());
                CuboidFace second = faceOf(atlas, edge.second());
                cross(atlas, first, edge.firstSide(), second, edge.secondSide(), edge.transform());
                cross(atlas, second, edge.secondSide(), first, edge.firstSide(), edge.transform().inverse());
            }
        }
    }

    private static void cross(CuboidAtlas atlas, CuboidFace from, Direction fromSide, CuboidFace to,
            Direction toSide, SeamTransform transform) {
        for (BlockPos inside : CuboidFixtures.blocksAlong(from, fromSide)) {
            BlockPos outside = inside.relative(fromSide);
            BlockPos landed = transform.apply(outside);
            assertSame(to, atlas.faceAt(landed.getX(), landed.getZ()));
            assertEquals(CuboidFixtures.sharedSegment(from, inside, fromSide),
                    CuboidFixtures.sharedSegment(to, landed, toSide));
            assertEquals(outside, transform.inverse().apply(landed));
        }
    }

    @Test
    void aWalkAroundEveryVertexIsAQuarterTurn() {
        for (CuboidAtlas atlas : atlases()) {
            Set<BoxVector> points = new HashSet<>();
            for (CuboidVertex vertex : atlas.vertices()) {
                points.add(vertex.point());
                List<Side> faces = vertex.faces();
                assertEquals(FACES_PER_VERTEX, faces.size());
                SeamTransform walk = SeamTransform.IDENTITY;
                for (int i = 0; i < FACES_PER_VERTEX; i++) {
                    walk = walk.then(crossing(atlas, faces.get(i), faces.get((i + 1) % FACES_PER_VERTEX)));
                }

                assertEquals(FoldOrientation.QUARTER_TURN_COUNTERCLOCKWISE, walk.orientation());
                for (Side side : faces) {
                    cornerAt(faceOf(atlas, side), vertex.point());
                }

                BlockPos corner = cornerAt(faceOf(atlas, faces.getFirst()), vertex.point());
                assertEquals(corner.getX(), walk.applyX(corner.getX(), corner.getZ()));
                assertEquals(corner.getZ(), walk.applyZ(corner.getX(), corner.getZ()));
            }

            assertEquals(VERTICES, points.size());
        }
    }

    private static SeamTransform crossing(CuboidAtlas atlas, Side from, Side to) {
        for (CuboidEdge edge : atlas.edges()) {
            if (edge.first() == from && edge.second() == to) {
                return edge.transform();
            }

            if (edge.first() == to && edge.second() == from) {
                return edge.transform().inverse();
            }
        }

        throw new IllegalArgumentException("No edge between " + from + " and " + to);
    }

    private static BlockPos cornerAt(CuboidFace face, BoxVector point) {
        for (int localX : new int[] {0, face.widthBlocks()}) {
            for (int localZ : new int[] {0, face.depthBlocks()}) {
                if (face.boxPoint(localX, localZ).equals(point)) {
                    return new BlockPos(face.blockX(localX), CuboidFixtures.BLOCK_Y, face.blockZ(localZ));
                }
            }
        }

        throw new IllegalArgumentException(face.side() + " has no corner at " + point);
    }
}
