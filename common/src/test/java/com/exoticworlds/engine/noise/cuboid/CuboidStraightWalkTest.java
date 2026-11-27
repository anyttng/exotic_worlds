package com.exoticworlds.engine.noise.cuboid;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.fold.cuboid.CuboidAtlas;
import com.exoticworlds.core.fold.cuboid.CuboidFace;
import com.exoticworlds.core.world.CuboidShape;

class CuboidStraightWalkTest {
    private static final int EDGE_CHUNKS = 16;
    private static final double EDGE_BLOCKS = 256.0;
    private static final double CENTER = 0.5;
    private static final double TWO_EDGES = 512.0;
    private static final double PAST_THE_VERTEX = 200.0;
    private static final double PAST_EACH_EDGE = 71.5;
    private static final double ONE_BLOCK = 1.0;
    private static final double EXACT = 1.0E-9;

    private static final CuboidAtlas ATLAS = new CuboidShape(EDGE_CHUNKS, EDGE_CHUNKS, EDGE_CHUNKS).atlas();

    private record Landed(CuboidFace.Side side, double localX, double localZ) {
    }

    private static Landed walkFromTheFrontCenter(double endX, double endZ) {
        Landed[] landed = new Landed[1];
        CuboidFace front = ATLAS.faceAt(0, 0);
        CuboidStraightWalk.of(ATLAS).land(ATLAS.faces().indexOf(front), CENTER, CENTER, endX, endZ,
                (atlasFace, localX, localZ) ->
                        landed[0] = new Landed(ATLAS.faces().get(atlasFace).side(), localX, localZ));
        return landed[0];
    }

    @Test
    void twoEdgesSouthOfTheFrontCenterIsTheBackCenter() {
        Landed landed = walkFromTheFrontCenter(CENTER, CENTER + TWO_EDGES);

        assertEquals(CuboidFace.Side.BACK, landed.side());
        assertEquals(EDGE_BLOCKS / 2.0, landed.localX(), ONE_BLOCK);
        assertEquals(EDGE_BLOCKS / 2.0, landed.localZ(), ONE_BLOCK);
    }

    @Test
    void aWalkThroughAVertexCrossesTheXEdgeFirst() {
        Landed landed = walkFromTheFrontCenter(CENTER - PAST_THE_VERTEX, CENTER - PAST_THE_VERTEX);

        assertEquals(CuboidFace.Side.TOP, landed.side());
        assertEquals(PAST_EACH_EDGE, landed.localX(), EXACT);
        assertEquals(EDGE_BLOCKS - PAST_EACH_EDGE, landed.localZ(), EXACT);
    }

    @Test
    void aWalkInsideOneFaceEndsWhereItPoints() {
        CuboidFace front = ATLAS.faceAt(0, 0);
        double endX = 100.5;
        double endZ = -90.5;

        Landed landed = walkFromTheFrontCenter(endX, endZ);

        assertEquals(CuboidFace.Side.FRONT, landed.side());
        assertEquals(endX - front.minBlockX(), landed.localX(), EXACT);
        assertEquals(endZ - front.minBlockZ(), landed.localZ(), EXACT);
    }
}
