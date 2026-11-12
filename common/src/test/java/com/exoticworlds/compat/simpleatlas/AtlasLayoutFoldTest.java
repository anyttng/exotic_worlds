package com.exoticworlds.compat.simpleatlas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.TestShapes;
import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.map.MapCopies;
import com.exoticworlds.compat.map.MapShapes;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;

import rubbertoe.simple_atlas.network.AtlasTilePayload;

class AtlasLayoutFoldTest {
    private static final String OVERWORLD = "minecraft:overworld";

    private static final String NETHER = "minecraft:the_nether";

    private static final int BLOCKS_PER_TILE = 128;

    private static final AxisBounds.Looped TINY = new AxisBounds.Looped(-16, 16);

    private static final ToroidalShape TORUS = TestShapes.of(WorldFolds.of(ShapeFixtures.torus(new WorldLoopBounds(TINY,
            TINY))));

    private static final ToroidalShape CYLINDER = TestShapes.of(WorldFolds.of(ShapeFixtures.cylinder(new WorldLoopBounds(
            TINY, AxisBounds.Unbounded.INSTANCE))));

    private static final ToroidalShape LATTICE = MapShapes.latticeTorus(-8, 8, 8);

    private static final List<AtlasTilePayload> ACROSS_THE_EDGE = List.of(
            tile(1, 256, 0), tile(2, 128, 0), tile(3, -128, 0));

    private static final List<AtlasTilePayload> ACROSS_THE_SKEWED_EDGE = List.of(tile(1, 0, 64), tile(2, -128, -64));

    @Test
    void repeatedLaysTheMapsAcrossTheEdgeSideBySide() {
        List<AtlasTilePayload> relaid = AtlasLayoutFold.relaid(ACROSS_THE_EDGE, AtlasLayoutFoldTest::blocksPerTile,
                shapes(TORUS), MapCopies.REPEATED);

        assertEquals(List.of(1, 0, 2), tileXs(relaid));
        assertEquals(List.of(0, 0, 0), tileYs(relaid));
    }

    @Test
    void singleKeepsEachMapWhereItWasMade() {
        List<AtlasTilePayload> relaid = AtlasLayoutFold.relaid(ACROSS_THE_EDGE, AtlasLayoutFoldTest::blocksPerTile,
                shapes(TORUS), MapCopies.SINGLE);

        assertEquals(List.of(3, 2, 0), tileXs(relaid));
    }

    @Test
    void theCentresStayAsTheServerSentThem() {
        List<AtlasTilePayload> relaid = AtlasLayoutFold.relaid(ACROSS_THE_EDGE, AtlasLayoutFoldTest::blocksPerTile,
                shapes(TORUS), MapCopies.REPEATED);

        assertEquals(List.of(256, 128, -128), relaid.stream().map(AtlasTilePayload::centerX).toList());
    }

    @Test
    void repeatedLaysTheMapPastTheZSeamOnItsSkewedCopy() {
        List<AtlasTilePayload> relaid = AtlasLayoutFold.relaid(ACROSS_THE_SKEWED_EDGE,
                AtlasLayoutFoldTest::blocksPerTile, shapes(LATTICE), MapCopies.REPEATED);

        assertEquals(List.of(0, 0), tileXs(relaid));
        assertEquals(List.of(0, 1), tileYs(relaid));
    }

    @Test
    void singleKeepsTheSkewedMapWhereItWasMade() {
        List<AtlasTilePayload> relaid = AtlasLayoutFold.relaid(ACROSS_THE_SKEWED_EDGE,
                AtlasLayoutFoldTest::blocksPerTile, shapes(LATTICE), MapCopies.SINGLE);

        assertEquals(List.of(1, 0), tileXs(relaid));
        assertEquals(List.of(1, 0), tileYs(relaid));
    }

    @Test
    void anOpenAxisKeepsItsDistances() {
        List<AtlasTilePayload> tiles = List.of(tile(1, 256, 1024), tile(2, 256, -1024));

        List<AtlasTilePayload> relaid = AtlasLayoutFold.relaid(tiles, AtlasLayoutFoldTest::blocksPerTile,
                shapes(CYLINDER), MapCopies.REPEATED);

        assertEquals(List.of(16, 0), tileYs(relaid));
    }

    @Test
    void anUnshapedDimensionKeepsTheTilesAsSent() {
        List<AtlasTilePayload> sent = List.of(new AtlasTilePayload(1, 0, 0, 0, 0, OVERWORLD),
                new AtlasTilePayload(2, 192, 192, 0, 0, NETHER), new AtlasTilePayload(3, 704, 192, 1, 0, NETHER));

        List<AtlasTilePayload> relaid = AtlasLayoutFold.relaid(sent, AtlasLayoutFoldTest::blocksPerTile,
                dimension -> null, MapCopies.REPEATED);

        assertEquals(sent, relaid);
    }

    @Test
    void eachShapedDimensionIsLaidOnItsOwnScale() {
        List<AtlasTilePayload> sent = List.of(tile(1, 256, 0), tile(2, 128, 0), tile(3, -128, 0),
                new AtlasTilePayload(4, 192, 192, 0, 0, NETHER), new AtlasTilePayload(5, 192, 704, 0, 1, NETHER));
        ToIntFunction<AtlasTilePayload> blocksPerTile = tile -> tile.dimension().equals(NETHER) ? 512 : 128;

        List<AtlasTilePayload> relaid = AtlasLayoutFold.relaid(sent, blocksPerTile,
                dimension -> dimension.equals(NETHER) ? CYLINDER : TORUS, MapCopies.REPEATED);

        assertEquals(List.of(1, 0, 2, 0, 0), tileXs(relaid));
        assertEquals(List.of(0, 0, 0, 0, 1), tileYs(relaid));
    }

    @Test
    void anUnshapedDimensionBesideAShapedOneKeepsItsTiles() {
        AtlasTilePayload nether = new AtlasTilePayload(4, 704, 192, 1, 0, NETHER);
        List<AtlasTilePayload> sent = List.of(tile(1, 256, 0), tile(2, 128, 0), tile(3, -128, 0), nether);

        List<AtlasTilePayload> relaid = AtlasLayoutFold.relaid(sent, AtlasLayoutFoldTest::blocksPerTile,
                dimension -> dimension.equals(NETHER) ? null : TORUS, MapCopies.REPEATED);

        assertSame(nether, relaid.get(3));
        assertEquals(List.of(1, 0, 2, 1), tileXs(relaid));
    }

    private static int blocksPerTile(AtlasTilePayload tile) {
        return BLOCKS_PER_TILE;
    }

    private static AtlasTilePayload tile(int mapId, int centerX, int centerZ) {
        return new AtlasTilePayload(mapId, centerX, centerZ, 0, 0, OVERWORLD);
    }

    private static Function<String, ToroidalShape> shapes(ToroidalShape shape) {
        return dimension -> shape;
    }

    private static List<Integer> tileXs(List<AtlasTilePayload> tiles) {
        return tiles.stream().map(AtlasTilePayload::tileX).toList();
    }

    private static List<Integer> tileYs(List<AtlasTilePayload> tiles) {
        return tiles.stream().map(AtlasTilePayload::tileY).toList();
    }
}
