package com.exoticworlds.compat.simpleatlas;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.TestShapes;
import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.MapCopies;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;

import rubbertoe.simple_atlas.network.AtlasTilePayload;

class AtlasLayoutFoldTest {
    private static final String OVERWORLD = "minecraft:overworld";

    private static final int BLOCKS_PER_TILE = 128;

    private static final AxisBounds.Looped TINY = new AxisBounds.Looped(-16, 16);

    private static final ToroidalShape TORUS = TestShapes.of(WorldFolds.of(FlatShape.torus(new WorldLoopBounds(TINY,
            TINY))));

    private static final ToroidalShape CYLINDER = TestShapes.of(WorldFolds.of(FlatShape.torus(new WorldLoopBounds(
            TINY, AxisBounds.Unbounded.INSTANCE))));

    private static final List<AtlasTilePayload> ACROSS_THE_EDGE = List.of(
            tile(1, 256, 0), tile(2, 128, 0), tile(3, -128, 0));

    @Test
    void repeatedLaysTheMapsAcrossTheEdgeSideBySide() {
        List<AtlasTilePayload> relaid = AtlasLayoutFold.relaid(ACROSS_THE_EDGE, BLOCKS_PER_TILE, shapes(TORUS),
                MapCopies.REPEATED);

        assertEquals(List.of(1, 0, 2), tileXs(relaid));
        assertEquals(List.of(0, 0, 0), tileYs(relaid));
    }

    @Test
    void singleKeepsEachMapWhereItWasMade() {
        List<AtlasTilePayload> relaid = AtlasLayoutFold.relaid(ACROSS_THE_EDGE, BLOCKS_PER_TILE, shapes(TORUS),
                MapCopies.SINGLE);

        assertEquals(List.of(3, 2, 0), tileXs(relaid));
    }

    @Test
    void theCentresStayAsTheServerSentThem() {
        List<AtlasTilePayload> relaid = AtlasLayoutFold.relaid(ACROSS_THE_EDGE, BLOCKS_PER_TILE, shapes(TORUS),
                MapCopies.REPEATED);

        assertEquals(List.of(256, 128, -128), relaid.stream().map(AtlasTilePayload::centerX).toList());
    }

    @Test
    void anOpenAxisKeepsItsDistances() {
        List<AtlasTilePayload> tiles = List.of(tile(1, 256, 1024), tile(2, 256, -1024));

        List<AtlasTilePayload> relaid = AtlasLayoutFold.relaid(tiles, BLOCKS_PER_TILE, shapes(CYLINDER),
                MapCopies.REPEATED);

        assertEquals(List.of(16, 0), tileYs(relaid));
    }

    @Test
    void aDimensionThatDoesNotWrapKeepsItsDistances() {
        List<AtlasTilePayload> relaid = AtlasLayoutFold.relaid(ACROSS_THE_EDGE, BLOCKS_PER_TILE, dimension -> null,
                MapCopies.REPEATED);

        assertEquals(List.of(3, 2, 0), tileXs(relaid));
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
