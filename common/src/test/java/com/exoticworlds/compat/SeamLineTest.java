package com.exoticworlds.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SeamLineTest {
    private static final int FTB_REGION_BLOCKS = 512;
    private static final int ATLAS_BLOCKS_PER_PIXEL = 4;
    private static final int ATLAS_TILE_MIN = -64;

    @Test
    void aVerticalEdgeRunsAlongZFromTheOrigin() {
        assertEquals(new SeamLine(true, 138, -44, 84),
                SeamLine.of(new WorldCopies.Edge(512, -256, 512, 256), 10, 20, 0.25),
                "x 512 at a quarter pixel per block lands 128 px right of the origin, z -256..256 spans 128 px");
    }

    @Test
    void aHorizontalEdgeRunsAlongXFromTheOrigin() {
        assertEquals(new SeamLine(false, 512, 0, 256),
                SeamLine.of(new WorldCopies.Edge(0, 1024, 512, 1024), 0, 0, 0.5),
                "z 1024 at half a pixel per block is row 512, x 0..512 spans 256 px");
    }

    @Test
    void aFractionalScaleRoundsToTheNearestPixel() {
        assertEquals(new SeamLine(true, 59, 0, 300),
                SeamLine.of(new WorldCopies.Edge(100, 0, 100, FTB_REGION_BLOCKS), 0, 0, 300 / (double) FTB_REGION_BLOCKS),
                "x 100 on a 300 px region tile is 58.59 px, drawn at 59");
    }

    @Test
    void anAtlasTileKeepsItsWholePixelPlacement() {
        int origin = -ATLAS_TILE_MIN / ATLAS_BLOCKS_PER_PIXEL;
        assertEquals(new SeamLine(true, (256 - ATLAS_TILE_MIN) / ATLAS_BLOCKS_PER_PIXEL, 0,
                        Math.ceilDiv(448 - ATLAS_TILE_MIN, ATLAS_BLOCKS_PER_PIXEL)),
                SeamLine.of(new WorldCopies.Edge(256, ATLAS_TILE_MIN, 256, 448), origin, origin,
                        1.0 / ATLAS_BLOCKS_PER_PIXEL),
                "a seam on a chunk bound of a scale-2 map tile lands where the tile's own floor and ceiling put it");
    }
}
