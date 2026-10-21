package com.exoticworlds.compat.ultimatemapatlases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.MapShapes;
import com.exoticworlds.compat.ultimatemapatlases.AtlasTileCopies.TileCenter;

import net.minecraft.core.BlockPos;

class AtlasTileCopiesTest {
    private static final int MAP_BLOCKS = 128;

    @Test
    void aKeyOnTheUpperBoundFindsTheMapHeldOnTheLowerBound() {
        AtlasTileCopies.TileLookup<TileCenter> held = holding(new TileCenter(-256, 0));
        ToroidalShape shape = MapShapes.torus(-16, 16);
        assertEquals(new TileCenter(-256, 0), AtlasTileCopies.first(shape, 256, 0, MAP_BLOCKS, held),
                "the key a player beside the upper bound gets is the held map's copy");
        assertEquals(new TileCenter(-256, 0), AtlasTileCopies.acrossEdge(shape, 256, 0, MAP_BLOCKS, held),
                "so making a map there would duplicate it");
        assertNull(AtlasTileCopies.acrossEdge(shape, -256, 0, MAP_BLOCKS, held),
                "the held map is not a copy of itself");
    }

    @Test
    void theKeyItselfIsAskedBeforeItsCopies() {
        AtlasTileCopies.TileLookup<TileCenter> held = holding(new TileCenter(256, 0), new TileCenter(-256, 0));
        assertEquals(new TileCenter(256, 0),
                AtlasTileCopies.first(MapShapes.torus(-16, 16), 256, 0, MAP_BLOCKS, held),
                "an atlas already holding both answers with the exact key");
    }

    @Test
    void aSkewedSeamFindsTheHeldMapMovedSideways() {
        ToroidalShape shape = MapShapes.latticeTorus(-16, 16, 8);
        BlockPos canonical = shape.fold(new BlockPos(0, 0, 512));
        TileCenter heldCenter = new TileCenter(canonical.getX(), canonical.getZ());
        AtlasTileCopies.TileLookup<TileCenter> held = holding(heldCenter);
        assertEquals(heldCenter, AtlasTileCopies.first(shape, 0, 512, MAP_BLOCKS, held),
                "a lap in Z finds the map the skew moved one map along X");
        assertEquals(heldCenter, AtlasTileCopies.acrossEdge(shape, 0, 512, MAP_BLOCKS, held),
                "and refuses a second map of that ground");
    }

    @Test
    void aCylinderFindsTheHeldMapAlongTheLoopedAxisAlone() {
        ToroidalShape shape = MapShapes.cylinder(-16, 16);
        AtlasTileCopies.TileLookup<TileCenter> held = holding(new TileCenter(-256, 5000));
        assertEquals(new TileCenter(-256, 5000), AtlasTileCopies.first(shape, 256, 5000, MAP_BLOCKS, held),
                "X folds a lap back");
        assertNull(AtlasTileCopies.first(shape, -256, 5512, MAP_BLOCKS, held), "the unbounded Z never folds");
    }

    @Test
    void noShapeFindsTheExactKeyAlone() {
        AtlasTileCopies.TileLookup<TileCenter> held = holding(new TileCenter(-256, 0));
        assertNull(AtlasTileCopies.first(null, 256, 0, MAP_BLOCKS, held), "an unwrapped level has no copies");
        assertNull(AtlasTileCopies.acrossEdge(null, -256, 0, MAP_BLOCKS, held),
                "and never refuses a map for one");
    }

    private static AtlasTileCopies.TileLookup<TileCenter> holding(TileCenter... centers) {
        Set<TileCenter> held = Set.of(centers);
        return (x, z) -> held.contains(new TileCenter(x, z)) ? new TileCenter(x, z) : null;
    }

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
    void aWorldNotWholeMapsWideDrawsTheMapsPastItsEdgeShifted() {
        ToroidalShape shape = MapShapes.torus(-18, 18);
        assertEquals(List.of(new TileCenter(384, 0)), AtlasTileCopies.of(shape, 384, 0, MAP_BLOCKS),
                "one 576-block lap is four and a half maps, so the grid finds no map past the edge");
        List<AtlasTileCopies.Placed<TileCenter>> placed = AtlasTileCopies.shifted(shape, new TileCenter(384, 0), 0,
                MAP_BLOCKS, holdingGrid(-256, 256));
        assertEquals(List.of(new AtlasTileCopies.Placed<>(new TileCenter(-256, 0), new TileCenter(320, 0)),
                new AtlasTileCopies.Placed<>(new TileCenter(-128, 0), new TileCenter(448, 0))), placed,
                "the two maps on the far side cover the tile, each a lap along X off the grid");
    }

    @Test
    void theRowPastASkewedSeamIsDrawnMovedSidewaysOffTheGrid() {
        ToroidalShape shape = MapShapes.latticeTorus(-16, 16, 4);
        int skew = shape.fold(new BlockPos(0, 0, 512)).getX();
        assertEquals(64, Math.abs(skew), "a lap in Z moves X by the 64-block skew");
        assertEquals(List.of(new TileCenter(0, 384)), AtlasTileCopies.of(shape, 0, 384, MAP_BLOCKS),
                "half a map sideways is no whole move, so the grid finds no map past the Z edge");
        List<AtlasTileCopies.Placed<TileCenter>> placed = AtlasTileCopies.shifted(shape, new TileCenter(0, 384), 0,
                MAP_BLOCKS, holdingGrid(-256, 256));
        assertTrue(placed.contains(new AtlasTileCopies.Placed<>(new TileCenter(skew - 64, -128),
                new TileCenter(-64, 384))), "the map behind the left half arrives moved sideways by the skew");
        assertTrue(placed.contains(new AtlasTileCopies.Placed<>(new TileCenter(skew + 64, -128),
                new TileCenter(64, 384))), "and so does the map behind the right half");
    }

    @Test
    void aCylinderShiftsTheLoopedAxisAlone() {
        List<AtlasTileCopies.Placed<TileCenter>> placed = AtlasTileCopies.shifted(MapShapes.cylinder(-18, 18),
                new TileCenter(384, 5120), 0, MAP_BLOCKS, holding(new TileCenter(-256, 5120), new TileCenter(-128, 5120)));
        assertEquals(List.of(new AtlasTileCopies.Placed<>(new TileCenter(-256, 5120), new TileCenter(320, 5120)),
                new AtlasTileCopies.Placed<>(new TileCenter(-128, 5120), new TileCenter(448, 5120))), placed,
                "X shifts a lap and the unbounded Z stays where the map is");
    }

    @Test
    void wholeMovesAreLeftToTheGrid() {
        assertEquals(List.of(), AtlasTileCopies.shifted(MapShapes.torus(-16, 16), new TileCenter(384, 0), 2,
                MAP_BLOCKS, holdingGrid(-256, 256)), "every lap of a 512-block world is whole maps");
        assertEquals(List.of(), AtlasTileCopies.shifted(null, new TileCenter(384, 0), 2, MAP_BLOCKS,
                holdingGrid(-256, 256)), "an unwrapped level and a single view shift nothing");
    }

    @Test
    void aTilePastTheEdgeIsCoveredOnlyWhereBothHalvesAreHeld() {
        ToroidalShape shape = MapShapes.torus(-18, 18);
        assertTrue(AtlasTileCopies.coveredByShifted(shape, new TileCenter(384, 0), MAP_BLOCKS,
                holding(new TileCenter(-256, 0), new TileCenter(-128, 0))),
                "blocks 320 to 448 are the ground of the maps at -256 and -128, a lap of 576 blocks back");
        assertFalse(AtlasTileCopies.coveredByShifted(shape, new TileCenter(384, 0), MAP_BLOCKS,
                holding(new TileCenter(-256, 0))), "the map at -256 holds only the half from 320 to 384");
    }

    @Test
    void aTileInsideTheWorldIsNeverCoveredByACopyReachingIntoIt() {
        ToroidalShape shape = MapShapes.torus(-18, 18);
        AtlasTileCopies.TileLookup<TileCenter> held = holding(new TileCenter(-256, 0));
        assertEquals(1, AtlasTileCopies.shifted(shape, new TileCenter(256, 0), 0, MAP_BLOCKS, held).size(),
                "the copy of the map at -256 lands at 320 and reaches into the tile");
        assertFalse(AtlasTileCopies.coveredByShifted(shape, new TileCenter(256, 0), MAP_BLOCKS, held),
                "but only the 64 blocks of it past the edge");
    }

    @Test
    void aSkewedRowPastTheZEdgeIsCoveredByTheMapsMovedSideways() {
        ToroidalShape shape = MapShapes.latticeTorus(-16, 16, 4);
        int skew = shape.fold(new BlockPos(0, 0, 512)).getX();
        assertTrue(AtlasTileCopies.coveredByShifted(shape, new TileCenter(0, 384), MAP_BLOCKS,
                holding(new TileCenter(skew - 64, -128), new TileCenter(skew + 64, -128))),
                "the two maps a row back, moved by the skew, meet in the middle of the tile");
        assertFalse(AtlasTileCopies.coveredByShifted(shape, new TileCenter(0, 384), MAP_BLOCKS,
                holding(new TileCenter(skew - 64, -128))), "one of them leaves half the tile");
    }

    private static AtlasTileCopies.TileLookup<TileCenter> holdingGrid(int from, int to) {
        return (x, z) -> x >= from && x <= to && z >= from && z <= to && x % MAP_BLOCKS == 0 && z % MAP_BLOCKS == 0
                ? new TileCenter(x, z) : null;
    }

    @Test
    void aCylinderMovesTheLoopedAxisAlone() {
        List<TileCenter> centers = AtlasTileCopies.of(MapShapes.cylinder(-16, 16), 768, 5000, MAP_BLOCKS);
        assertTrue(centers.contains(new TileCenter(256, 5000)), "X folds a lap back");
        assertTrue(centers.contains(new TileCenter(-256, 5000)), "and two laps back");
        assertFalse(centers.stream().anyMatch(center -> center.z() != 5000), "the unbounded Z never moves");
    }
}
