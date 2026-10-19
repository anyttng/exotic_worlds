package com.exoticworlds.compat.xaero;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.MapCopies;
import com.exoticworlds.compat.MapShapes;
import com.exoticworlds.compat.MapViewSpan;
import com.exoticworlds.compat.xaero.XaeroMapView.SeamFill;

import net.minecraft.core.Direction;

class XaeroMapViewTest {
    private static final ToroidalShape SMALL = MapShapes.torus(-16, 16);
    private static final int WINDOW = 1024;
    private static final double HALF_SPAN = MapViewSpan.halfBlocks(100, 1.0);

    @Test
    void theSeamGridFillsAOneBlockLineAtEachXSeam() {
        List<SeamFill> fills = XaeroMapView.seamFills(SMALL, true, new XaeroCamera(0.0, 0.0, 1.0), WINDOW, WINDOW);
        assertEquals(Set.of(-256, 256), verticalAt(fills, 1), "the X seams of a 512-block world seen from its center");
    }

    @Test
    void aZoomedOutGridThickensTheLineToOnePixel() {
        List<SeamFill> fills = XaeroMapView.seamFills(SMALL, true, new XaeroCamera(0.0, 0.0, 0.25), WINDOW, WINDOW);
        assertTrue(verticalAt(fills, 4).contains(256), "at a quarter pixel per block a line is 4 blocks wide");
        assertEquals(Set.of(), verticalAt(fills, 1));
    }

    @Test
    void aGridTurnedOffAndAnUnshapedWorldDrawNothing() {
        assertEquals(List.of(), XaeroMapView.seamFills(SMALL, false, new XaeroCamera(0.0, 0.0, 1.0), WINDOW, WINDOW));
        assertEquals(List.of(), XaeroMapView.seamFills(null, true, new XaeroCamera(0.0, 0.0, 1.0), WINDOW, WINDOW));
    }

    @Test
    void aSingleCopyViewHoldsTheCameraHalfAWindowInsideTheEdge() {
        assertEquals(206.0, XaeroMapView.clampCamera(SMALL, MapCopies.SINGLE, Direction.Axis.X, 250.0, HALF_SPAN),
                "the world ends at 256 and the window reaches 50 blocks either side");
    }

    @Test
    void aRepeatedViewAndAnUnshapedWorldLeaveTheCameraFree() {
        assertEquals(250.0, XaeroMapView.clampCamera(SMALL, MapCopies.REPEATED, Direction.Axis.X, 250.0, HALF_SPAN));
        assertEquals(250.0, XaeroMapView.clampCamera(null, MapCopies.SINGLE, Direction.Axis.X, 250.0, HALF_SPAN));
    }

    @Test
    void theZoomFloorOnlyEverRaisesTheScale() {
        assertEquals(0.5, XaeroMapView.raised(0.25, 0.5));
        assertEquals(2.0, XaeroMapView.raised(2.0, 0.5));
        assertEquals(0.25, XaeroMapView.raised(0.25, 0.0), "no floor on a world without a shape");
    }

    private static Set<Integer> verticalAt(List<SeamFill> fills, int thickness) {
        Set<Integer> xs = new HashSet<>();
        for (SeamFill fill : fills) {
            if (fill.maxX() - fill.minX() == thickness && fill.maxZ() - fill.minZ() > thickness) {
                xs.add(fill.minX());
            }
        }

        return xs;
    }
}
