package com.exoticworlds.compat.ultimatemapatlases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.compat.MapShapes;
import com.exoticworlds.compat.SeamLine;

class AtlasSeamLinesTest {
    @Test
    void aTileOnTheSeamCarriesItsEdge() {
        List<SeamLine> lines = AtlasSeamLines.of(MapShapes.torus(-16, 16), 256, 0, 128);
        assertTrue(lines.contains(new SeamLine(true, 64, 0, 128)),
                "the map centered on the upper bound draws the seam down its middle column");
    }

    @Test
    void aTileInsideTheWorldCarriesNoEdge() {
        assertEquals(List.of(), AtlasSeamLines.of(MapShapes.torus(-16, 16), 0, 0, 128),
                "no seam crosses the map at the world's middle");
    }

    @Test
    void aWiderMapPlacesItsEdgeInItsOwnPixels() {
        List<SeamLine> lines = AtlasSeamLines.of(MapShapes.torus(-16, 16), 192, 192, 256);
        assertTrue(lines.contains(new SeamLine(true, 96, 0, 96)),
                "a 256-block map at 192 meets the X seam 192 blocks from its edge, 96 px at 2 blocks per pixel,"
                        + " down to the Z seam at the same row");
    }
}
