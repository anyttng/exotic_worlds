package com.exoticworlds.compat.ultimatemapatlases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.map.MapShapes;
import com.exoticworlds.compat.map.SeamLine;
import com.exoticworlds.compat.ultimatemapatlases.AtlasTileCopies.TileCenter;

class AtlasTileMemoTest {
    private static final int MAP_BLOCKS = 128;

    @Test
    void aHeldTileAnswersWhatTheLookupsAnswer() {
        ToroidalShape shape = MapShapes.torus(-16, 16);
        AtlasTileMemo memo = new AtlasTileMemo();
        for (int ask = 0; ask < 2; ask++) {
            assertEquals(AtlasTileCopies.of(shape, 768, 0, MAP_BLOCKS), memo.copies(shape, 768, 0, MAP_BLOCKS),
                    "the copies of a tile, asked " + (ask + 1) + " times");
            assertEquals(AtlasSeamLines.of(shape, 256, 0, MAP_BLOCKS), memo.seamLines(shape, 256, 0, MAP_BLOCKS),
                    "the seam lines of a tile, asked " + (ask + 1) + " times");
        }

        List<TileCenter> first = memo.copies(shape, 768, 0, MAP_BLOCKS);
        assertSame(first, memo.copies(shape, 768, 0, MAP_BLOCKS), "a second ask of one tile is the held list");
    }

    @Test
    void tilesThatShareOneCoordinateAreHeldApart() {
        ToroidalShape shape = MapShapes.torus(-16, 16);
        AtlasTileMemo memo = new AtlasTileMemo();
        assertEquals(AtlasTileCopies.of(shape, 256, -256, MAP_BLOCKS), memo.copies(shape, 256, -256, MAP_BLOCKS));
        assertEquals(AtlasTileCopies.of(shape, -256, 256, MAP_BLOCKS), memo.copies(shape, -256, 256, MAP_BLOCKS),
                "a negative coordinate keeps its own tile");
        assertEquals(AtlasTileCopies.of(shape, 256, 256, MAP_BLOCKS), memo.copies(shape, 256, 256, MAP_BLOCKS));
    }

    @Test
    void anotherShapeDropsWhatWasHeld() {
        ToroidalShape torus = MapShapes.torus(-16, 16);
        ToroidalShape cylinder = MapShapes.cylinder(-16, 16);
        AtlasTileMemo memo = new AtlasTileMemo();
        memo.copies(torus, 256, 768, MAP_BLOCKS);
        List<SeamLine> torusLines = memo.seamLines(torus, 256, 256, MAP_BLOCKS);
        assertEquals(AtlasTileCopies.of(cylinder, 256, 768, MAP_BLOCKS), memo.copies(cylinder, 256, 768, MAP_BLOCKS),
                "the cylinder's copies, not the torus's");
        assertEquals(AtlasSeamLines.of(cylinder, 256, 256, MAP_BLOCKS), memo.seamLines(cylinder, 256, 256, MAP_BLOCKS),
                "the cylinder's seam lines, not the torus's");
        assertTrue(torusLines.size() > memo.seamLines(cylinder, 256, 256, MAP_BLOCKS).size(),
                "the two shapes draw different seams on this tile, so a kept list would show");
        assertEquals(List.of(new TileCenter(256, 768)), memo.copies(null, 256, 768, MAP_BLOCKS),
                "and no shape asks the request alone");
    }

    @Test
    void anotherMapSizeDropsWhatWasHeld() {
        ToroidalShape shape = MapShapes.torus(-16, 16);
        AtlasTileMemo memo = new AtlasTileMemo();
        memo.copies(shape, 1472, 0, MAP_BLOCKS);
        assertEquals(AtlasTileCopies.of(shape, 1472, 0, 1024), memo.copies(shape, 1472, 0, 1024),
                "the same center at a larger map size has its own copies");
    }

    @Test
    void aViewDraggedWithoutEndNeverHoldsMoreThanTheCeiling() {
        ToroidalShape shape = MapShapes.torus(-16, 16);
        AtlasTileMemo memo = new AtlasTileMemo();
        List<TileCenter> first = memo.copies(shape, 0, 0, MAP_BLOCKS);
        for (int tile = 1; tile < AtlasTileMemo.MAX_TILES; tile++) {
            int centerX = tile * MAP_BLOCKS;
            assertEquals(AtlasTileCopies.of(shape, centerX, 0, MAP_BLOCKS), memo.copies(shape, centerX, 0, MAP_BLOCKS));
        }

        assertSame(first, memo.copies(shape, 0, 0, MAP_BLOCKS), "the ceiling's worth of tiles is all held");
        memo.copies(shape, AtlasTileMemo.MAX_TILES * MAP_BLOCKS, 0, MAP_BLOCKS);
        List<TileCenter> again = memo.copies(shape, 0, 0, MAP_BLOCKS);
        assertNotSame(first, again, "one tile past the ceiling dropped what was held");
        assertEquals(first, again, "and the tile is answered afresh with the same centers");
    }
}
