package com.exoticworlds.compat.ultimatemapatlases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.MapShapes;
import com.exoticworlds.compat.ultimatemapatlases.AtlasTileCopies.TileCenter;

import net.minecraft.core.BlockPos;

class AtlasTileCopiesTest {
    private static final int MAP_BLOCKS = 128;

    @Test
    void aRequestPastTheSeamFindsItsCanonicalCenters() {
        List<TileCenter> centers = AtlasTileCopies.of(MapShapes.torus(-16, 16), 768, 0, MAP_BLOCKS);
        assertEquals(new TileCenter(768, 0), centers.get(0), "the request itself is asked first");
        assertTrue(centers.contains(new TileCenter(256, 0)),
                "one lap back the request is the map whose center sits on the upper bound");
        assertTrue(centers.contains(new TileCenter(-256, 0)), "two laps back it is the map on the lower bound");
    }

    @Test
    void aRequestInsideTheWorldKeepsItsOwnCenterFirst() {
        List<TileCenter> centers = AtlasTileCopies.of(MapShapes.torus(-16, 16), 256, 0, MAP_BLOCKS);
        assertEquals(new TileCenter(256, 0), centers.get(0), "a map on the upper bound answers for itself");
        assertTrue(centers.contains(new TileCenter(-256, 0)), "its twin on the lower bound covers the same ground");
    }

    @Test
    void noShapeAsksTheRequestAlone() {
        assertEquals(List.of(new TileCenter(768, 0)), AtlasTileCopies.of(null, 768, 0, MAP_BLOCKS),
                "an unwrapped level and a single view ask the request alone");
    }

    @Test
    void aMoveThatIsNotWholeMapsIsDropped() {
        List<TileCenter> centers = AtlasTileCopies.of(MapShapes.torus(-16, 16), 1472, 0, 1024);
        assertFalse(centers.contains(new TileCenter(960, 0)), "one 512-block lap is half a 1024-block map");
        assertTrue(centers.contains(new TileCenter(448, 0)), "two laps are one whole map");
    }

    @Test
    void theRowPastASkewedSeamArrivesMovedSideways() {
        ToroidalShape shape = MapShapes.latticeTorus(-16, 16, 8);
        BlockPos canonical = shape.fold(new BlockPos(0, 0, 512));
        List<TileCenter> centers = AtlasTileCopies.of(shape, 0, 512, MAP_BLOCKS);
        assertEquals(128, Math.abs(canonical.getX()), "a lap in Z moves X by the 128-block skew");
        assertTrue(centers.contains(new TileCenter(canonical.getX(), canonical.getZ())),
                "a skew of one whole map keeps the canonical center");
        assertEquals(List.of(new TileCenter(0, 512)), AtlasTileCopies.of(shape, 0, 512, 2 * MAP_BLOCKS),
                "a skew of half a map leaves no whole move");
    }

    @Test
    void aCylinderMovesTheLoopedAxisAlone() {
        List<TileCenter> centers = AtlasTileCopies.of(MapShapes.cylinder(-16, 16), 768, 5000, MAP_BLOCKS);
        assertTrue(centers.contains(new TileCenter(256, 5000)), "X folds a lap back");
        assertTrue(centers.contains(new TileCenter(-256, 5000)), "and two laps back");
        assertFalse(centers.stream().anyMatch(center -> center.z() != 5000), "the unbounded Z never moves");
    }
}
