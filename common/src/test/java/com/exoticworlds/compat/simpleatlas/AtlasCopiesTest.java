package com.exoticworlds.compat.simpleatlas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.MapShapes;

class AtlasCopiesTest {
    private static final int TILE_BLOCKS = 128;

    private static final float PIXELS_PER_BLOCK = 40.0F / TILE_BLOCKS;

    private static final ToroidalShape ONE_TILE_TORUS = MapShapes.torus(-4, 4);

    private static final ToroidalShape UNEVEN_TORUS = MapShapes.torus(-17, 17);

    private static final AtlasCopies.Layout ONE_TILE = new AtlasCopies.Layout(0.0F, 0.0F, 40.0F, 40.0F, -64, -64,
            PIXELS_PER_BLOCK);

    private static final AtlasCopies.Layout TWO_TILES = new AtlasCopies.Layout(0.0F, 0.0F, 80.0F, 80.0F, -128, -128,
            PIXELS_PER_BLOCK);

    private static final AtlasView WIDE = new AtlasView(-50.0F, -50.0F, 150.0F, 150.0F);

    @Test
    void anAreaAsWideAsTheBaseShowsTheBaseAlone() {
        assertEquals(AtlasCopies.BASE_ONLY, AtlasCopies.visible(ONE_TILE_TORUS, ONE_TILE,
                new AtlasView(0.0F, 0.0F, 40.0F, 40.0F), TILE_BLOCKS));
    }

    @Test
    void aCylinderRepeatsOnItsLoopingAxisOnly() {
        List<AtlasCopies.Offset> offsets = AtlasCopies.visible(MapShapes.cylinder(-4, 4), ONE_TILE,
                new AtlasView(20.0F, 20.0F, 40.0F, 40.0F), TILE_BLOCKS);

        assertEquals(List.of(AtlasCopies.BASE, new AtlasCopies.Offset(40.0F, 0.0F)), offsets);
    }

    @Test
    void aWideAreaFillsWithCopiesOnBothAxes() {
        List<AtlasCopies.Offset> offsets = AtlasCopies.visible(ONE_TILE_TORUS, ONE_TILE, WIDE, TILE_BLOCKS);

        assertEquals(25, offsets.size());
        assertEquals(new AtlasCopies.Offset(-80.0F, -80.0F), offsets.getFirst());
        assertEquals(new AtlasCopies.Offset(80.0F, 80.0F), offsets.getLast());
    }

    @Test
    void aWidthThatIsNotAWholeNumberOfMapsDrawsNoCopies() {
        assertEquals(AtlasCopies.BASE_ONLY, AtlasCopies.visible(UNEVEN_TORUS, ONE_TILE,
                new AtlasView(-500.0F, -500.0F, 1000.0F, 1000.0F), TILE_BLOCKS));
        assertEquals(AtlasCopies.BASE_ONLY, AtlasCopies.visible(null, ONE_TILE, WIDE, TILE_BLOCKS));
    }

    @Test
    void anyMoveDrawsTheCopyOneUnevenLapOver() {
        List<AtlasCopies.Offset> offsets = AtlasCopies.visible(UNEVEN_TORUS, ONE_TILE,
                new AtlasView(150.0F, 0.0F, 40.0F, 40.0F), AtlasCopies.ANY_MOVE);

        assertEquals(List.of(new AtlasCopies.Offset(170.0F, 0.0F)), offsets);
    }

    @Test
    void theRowPastTheZSeamSitsTheSkewOver() {
        List<AtlasCopies.Offset> offsets = AtlasCopies.visible(MapShapes.latticeTorus(-8, 8, 8), TWO_TILES,
                new AtlasView(0.0F, 80.0F, 80.0F, 80.0F), TILE_BLOCKS);

        assertEquals(List.of(new AtlasCopies.Offset(-40.0F, 80.0F), new AtlasCopies.Offset(40.0F, 80.0F)), offsets);
    }

    @Test
    void aSkewOfHalfAMapDrawsOnlyTheRowsItMovesByWholeMaps() {
        List<AtlasCopies.Offset> offsets = AtlasCopies.visible(MapShapes.latticeTorus(-8, 8, 4), TWO_TILES,
                new AtlasView(-100.0F, -100.0F, 300.0F, 300.0F), TILE_BLOCKS);

        assertTrue(offsets.stream().allMatch(offset -> offset.y() % 160.0F == 0.0F), offsets::toString);
        assertTrue(offsets.stream().anyMatch(offset -> offset.y() == 160.0F), offsets::toString);
    }

    @Test
    void aPointerOverACopyLandsOnTheSameSpotOfTheBase() {
        List<AtlasCopies.Offset> copies = AtlasCopies.visible(ONE_TILE_TORUS, ONE_TILE, WIDE, TILE_BLOCKS);

        assertEquals(new AtlasCopies.Offset(80.0F, 0.0F), AtlasCopies.under(95.0, 15.0, copies, ONE_TILE));
        assertEquals(new AtlasCopies.Offset(-40.0F, -40.0F), AtlasCopies.under(-5.0, -5.0, copies, ONE_TILE));
        assertEquals(AtlasCopies.BASE, AtlasCopies.under(20.0, 20.0, copies, ONE_TILE));
    }

    @Test
    void aPointerOverTheSkewedRowLandsOnTheBaseByTheSameMove() {
        List<AtlasCopies.Offset> copies = AtlasCopies.visible(MapShapes.latticeTorus(-8, 8, 8), TWO_TILES,
                new AtlasView(0.0F, 80.0F, 80.0F, 80.0F), TILE_BLOCKS);

        assertEquals(new AtlasCopies.Offset(40.0F, 80.0F), AtlasCopies.under(50.0, 90.0, copies, TWO_TILES));
    }

    @Test
    void aPointerOffTheBaseAndOffEveryCopyStaysWhereItIs() {
        assertEquals(AtlasCopies.BASE, AtlasCopies.under(30.0, 30.0, AtlasCopies.BASE_ONLY,
                new AtlasCopies.Layout(0.0F, 0.0F, 20.0F, 20.0F, -64, -64, PIXELS_PER_BLOCK)));
    }
}
