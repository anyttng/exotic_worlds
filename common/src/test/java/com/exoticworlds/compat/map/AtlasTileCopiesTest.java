package com.exoticworlds.compat.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import com.exoticworlds.compat.map.AtlasTileCopies.TileCenter;
import com.exoticworlds.core.fold.WorldFold;

import net.minecraft.core.BlockPos;

class AtlasTileCopiesTest {
    private static final int MAP_BLOCKS = 128;
    private static final int MAX_MAP_SCALE = 4;
    private static final WorldFold OFF_GRID = MapFolds.torus(-937, 938);

    @Test
    void aKeyOnTheUpperBoundFindsTheMapHeldOnTheLowerBound() {
        AtlasTileCopies.TileLookup<TileCenter> held = holding(new TileCenter(-256, 0));
        WorldFold shape = MapFolds.torus(-16, 16);
        assertEquals(new TileCenter(-256, 0), firstHeld(shape, 256, 0, MAP_BLOCKS, held),
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
                firstHeld(MapFolds.torus(-16, 16), 256, 0, MAP_BLOCKS, held),
                "an atlas already holding both answers with the exact key");
    }

    @Test
    void aSkewedSeamFindsTheHeldMapMovedSideways() {
        WorldFold shape = MapFolds.latticeTorus(-16, 16, 8);
        BlockPos canonical = shape.fold(new BlockPos(0, 0, 512));
        TileCenter heldCenter = new TileCenter(canonical.getX(), canonical.getZ());
        AtlasTileCopies.TileLookup<TileCenter> held = holding(heldCenter);
        assertEquals(heldCenter, firstHeld(shape, 0, 512, MAP_BLOCKS, held),
                "a lap in Z finds the map the skew moved one map along X");
        assertEquals(heldCenter, AtlasTileCopies.acrossEdge(shape, 0, 512, MAP_BLOCKS, held),
                "and refuses a second map of that ground");
    }

    @Test
    void aCylinderFindsTheHeldMapAlongTheLoopedAxisAlone() {
        WorldFold shape = MapFolds.cylinder(-16, 16);
        AtlasTileCopies.TileLookup<TileCenter> held = holding(new TileCenter(-256, 5000));
        assertEquals(new TileCenter(-256, 5000), firstHeld(shape, 256, 5000, MAP_BLOCKS, held),
                "X folds a lap back");
        assertNull(firstHeld(shape, -256, 5512, MAP_BLOCKS, held), "the unbounded Z never folds");
    }

    @Test
    void noShapeFindsTheExactKeyAlone() {
        AtlasTileCopies.TileLookup<TileCenter> held = holding(new TileCenter(-256, 0));
        assertNull(firstHeld(null, 256, 0, MAP_BLOCKS, held), "an unwrapped level has no copies");
        assertNull(AtlasTileCopies.acrossEdge(null, -256, 0, MAP_BLOCKS, held),
                "and never refuses a map for one");
    }

    private static @Nullable TileCenter firstHeld(@Nullable WorldFold shape, int x, int z, int mapBlocks,
            AtlasTileCopies.TileLookup<TileCenter> held) {
        for (TileCenter copy : new AtlasTileMemo().copies(shape, x, z, mapBlocks)) {
            TileCenter found = held.at(copy.x(), copy.z());
            if (found != null) {
                return found;
            }
        }

        return null;
    }

    private static AtlasTileCopies.TileLookup<TileCenter> holding(TileCenter... centers) {
        Set<TileCenter> held = Set.of(centers);
        return (x, z) -> held.contains(new TileCenter(x, z)) ? new TileCenter(x, z) : null;
    }

    @Test
    void aRequestPastTheSeamFindsItsCanonicalCenters() {
        List<TileCenter> centers = AtlasTileCopies.of(MapFolds.torus(-16, 16), 768, 0, MAP_BLOCKS);
        assertEquals(new TileCenter(768, 0), centers.get(0), "the request itself is asked first");
        assertTrue(centers.contains(new TileCenter(256, 0)),
                "one lap back the request is the map whose center sits on the upper bound");
        assertTrue(centers.contains(new TileCenter(-256, 0)), "two laps back it is the map on the lower bound");
    }

    @Test
    void aRequestInsideTheWorldKeepsItsOwnCenterFirst() {
        List<TileCenter> centers = AtlasTileCopies.of(MapFolds.torus(-16, 16), 256, 0, MAP_BLOCKS);
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
        List<TileCenter> centers = AtlasTileCopies.of(MapFolds.torus(-16, 16), 1472, 0, 1024);
        assertFalse(centers.contains(new TileCenter(960, 0)), "one 512-block lap is half a 1024-block map");
        assertTrue(centers.contains(new TileCenter(448, 0)), "two laps are one whole map");
    }

    @Test
    void theRowPastASkewedSeamArrivesMovedSideways() {
        WorldFold shape = MapFolds.latticeTorus(-16, 16, 8);
        BlockPos canonical = shape.fold(new BlockPos(0, 0, 512));
        List<TileCenter> centers = AtlasTileCopies.of(shape, 0, 512, MAP_BLOCKS);
        assertEquals(128, Math.abs(canonical.getX()), "a lap in Z moves X by the 128-block skew");
        assertTrue(centers.contains(new TileCenter(canonical.getX(), canonical.getZ())),
                "a skew of one whole map keeps the canonical center");
        List<TileCenter> wide = AtlasTileCopies.of(shape, 0, 512, 2 * MAP_BLOCKS);
        assertFalse(wide.contains(new TileCenter(canonical.getX(), canonical.getZ())),
                "one lap moves X by half a 256-block map");
        assertTrue(wide.contains(new TileCenter(2 * canonical.getX(), canonical.getZ() - 512)),
                "two laps move it a whole map and land a map past the lower bound, as far as the ring of neighbors"
                        + " reaches");
    }

    @Test
    void aRequestInsideAWholeMapWorldFindsTheNeighborMadePastEitherBound() {
        WorldFold shape = MapFolds.torus(-32, 32);
        assertTrue(AtlasTileCopies.of(shape, 384, 0, MAP_BLOCKS).contains(new TileCenter(-640, 0)),
                "the map at -640 lies wholly past the lower bound at -512, a lap along X from 384");
        assertTrue(AtlasTileCopies.of(shape, -384, 0, MAP_BLOCKS).contains(new TileCenter(640, 0)),
                "and the map at 640 wholly past the upper bound at 512, a lap from -384");
    }

    @Test
    void aWorldNotWholeMapsWideDrawsTheMapsPastItsEdgeShifted() {
        WorldFold shape = MapFolds.torus(-18, 18);
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
        WorldFold shape = MapFolds.latticeTorus(-16, 16, 4);
        int skewBlocks = shape.fold(new BlockPos(0, 0, 512)).getX();
        assertEquals(64, Math.abs(skewBlocks), "a lap in Z moves X by the 64-block skew");
        assertEquals(List.of(new TileCenter(0, 384)), AtlasTileCopies.of(shape, 0, 384, MAP_BLOCKS),
                "half a map sideways is no whole move, so the grid finds no map past the Z edge");
        List<AtlasTileCopies.Placed<TileCenter>> placed = AtlasTileCopies.shifted(shape, new TileCenter(0, 384), 0,
                MAP_BLOCKS, holdingGrid(-256, 256));
        assertTrue(placed.contains(new AtlasTileCopies.Placed<>(new TileCenter(skewBlocks - 64, -128),
                new TileCenter(-64, 384))), "the map behind the left half arrives moved sideways by the skew");
        assertTrue(placed.contains(new AtlasTileCopies.Placed<>(new TileCenter(skewBlocks + 64, -128),
                new TileCenter(64, 384))), "and so does the map behind the right half");
    }

    @Test
    void aCylinderShiftsTheLoopedAxisAlone() {
        List<AtlasTileCopies.Placed<TileCenter>> placed = AtlasTileCopies.shifted(MapFolds.cylinder(-18, 18),
                new TileCenter(384, 5120), 0, MAP_BLOCKS, holding(new TileCenter(-256, 5120), new TileCenter(-128, 5120)));
        assertEquals(List.of(new AtlasTileCopies.Placed<>(new TileCenter(-256, 5120), new TileCenter(320, 5120)),
                new AtlasTileCopies.Placed<>(new TileCenter(-128, 5120), new TileCenter(448, 5120))), placed,
                "X shifts a lap and the unbounded Z stays where the map is");
    }

    @Test
    void wholeMovesAreLeftToTheGrid() {
        assertEquals(List.of(), AtlasTileCopies.shifted(MapFolds.torus(-16, 16), new TileCenter(384, 0), 2,
                MAP_BLOCKS, holdingGrid(-256, 256)), "every lap of a 512-block world is whole maps");
        assertEquals(List.of(), AtlasTileCopies.shifted(null, new TileCenter(384, 0), 2, MAP_BLOCKS,
                holdingGrid(-256, 256)), "an unwrapped level and a single view shift nothing");
    }

    @Test
    void aTilePastTheEdgeIsCoveredOnlyWhereBothHalvesAreHeld() {
        WorldFold shape = MapFolds.torus(-18, 18);
        assertTrue(AtlasTileCopies.isCoveredByShifted(shape, new TileCenter(384, 0), MAP_BLOCKS,
                holding(new TileCenter(-256, 0), new TileCenter(-128, 0))),
                "blocks 320 to 448 are the ground of the maps at -256 and -128, a lap of 576 blocks back");
        assertFalse(AtlasTileCopies.isCoveredByShifted(shape, new TileCenter(384, 0), MAP_BLOCKS,
                holding(new TileCenter(-256, 0))), "the map at -256 holds only the half from 320 to 384");
    }

    @Test
    void aTileInsideTheWorldIsNeverCoveredByACopyReachingIntoIt() {
        WorldFold shape = MapFolds.torus(-18, 18);
        AtlasTileCopies.TileLookup<TileCenter> held = holding(new TileCenter(-256, 0));
        assertEquals(1, AtlasTileCopies.shifted(shape, new TileCenter(256, 0), 0, MAP_BLOCKS, held).size(),
                "the copy of the map at -256 lands at 320 and reaches into the tile");
        assertFalse(AtlasTileCopies.isCoveredByShifted(shape, new TileCenter(256, 0), MAP_BLOCKS, held),
                "but only the 64 blocks of it past the edge");
    }

    @Test
    void aSkewedRowPastTheZEdgeIsCoveredByTheMapsMovedSideways() {
        WorldFold shape = MapFolds.latticeTorus(-16, 16, 4);
        int skewBlocks = shape.fold(new BlockPos(0, 0, 512)).getX();
        assertTrue(AtlasTileCopies.isCoveredByShifted(shape, new TileCenter(0, 384), MAP_BLOCKS,
                holding(new TileCenter(skewBlocks - 64, -128), new TileCenter(skewBlocks + 64, -128))),
                "the two maps a row back, moved by the skew, meet in the middle of the tile");
        assertFalse(AtlasTileCopies.isCoveredByShifted(shape, new TileCenter(0, 384), MAP_BLOCKS,
                holding(new TileCenter(skewBlocks - 64, -128))), "one of them leaves half the tile");
    }

    private static AtlasTileCopies.TileLookup<TileCenter> holdingGrid(int from, int to) {
        return (x, z) -> x >= from && x <= to && z >= from && z <= to && x % MAP_BLOCKS == 0 && z % MAP_BLOCKS == 0
                ? new TileCenter(x, z) : null;
    }

    @Test
    void aCylinderMovesTheLoopedAxisAlone() {
        List<TileCenter> centers = AtlasTileCopies.of(MapFolds.cylinder(-16, 16), 768, 5000, MAP_BLOCKS);
        assertTrue(centers.contains(new TileCenter(256, 5000)), "X folds a lap back");
        assertTrue(centers.contains(new TileCenter(-256, 5000)), "and two laps back");
        assertFalse(centers.stream().anyMatch(center -> center.z() != 5000), "the unbounded Z never moves");
    }

    @Test
    void aWorldNoMapSizeDividesAnswersTheRequestAlone() {
        for (int scale = 0; scale <= MAX_MAP_SCALE; scale++) {
            int mapBlocks = MAP_BLOCKS << scale;
            assertEquals(List.of(new TileCenter(14976, 14976)),
                    AtlasTileCopies.of(OFF_GRID, 14976, 14976, mapBlocks),
                    "a 30000-block lap is no whole number of " + mapBlocks + "-block maps on either axis");
        }
    }

    @Test
    void theCopiesPastTheEdgeOfAWideWorldFollowTheView() {
        TileCenter edge = new TileCenter(14976, 0);
        AtlasTileCopies.TileLookup<TileCenter> held = holdingGrid(-15000, 15000);
        assertEquals(List.of(new AtlasTileCopies.Placed<>(new TileCenter(-14976, 0), new TileCenter(15024, 0))),
                AtlasTileCopies.shifted(OFF_GRID, edge, 0, MAP_BLOCKS, held),
                "the tile across the seam at 15008 meets one map past it, the map on the lower bound a lap along X");
        List<AtlasTileCopies.Placed<TileCenter>> ring = AtlasTileCopies.shifted(OFF_GRID, edge, 1, MAP_BLOCKS, held);
        assertEquals(6, ring.size(), "the ring around it meets two columns past the seam, three rows each");
        assertTrue(ring.stream().allMatch(copy -> copy.at().x() - copy.map().x() == 30000
                && copy.at().z() == copy.map().z()), "every one moved a lap along X");
    }

    @Test
    void aMapWhollyPastTheLowerBoundCoversATileBesideTheUpperBound() {
        TileCenter edge = new TileCenter(14976, 0);
        AtlasTileCopies.TileLookup<TileCenter> held = holding(new TileCenter(-15104, 0), new TileCenter(-14976, 0));
        assertEquals(List.of(new AtlasTileCopies.Placed<>(new TileCenter(-15104, 0), new TileCenter(14896, 0)),
                new AtlasTileCopies.Placed<>(new TileCenter(-14976, 0), new TileCenter(15024, 0))),
                AtlasTileCopies.shifted(OFF_GRID, edge, 0, MAP_BLOCKS, held),
                "blocks -15168 to -15040 lie wholly past the lower bound at -14992, and a lap along X they hold"
                        + " 14832 to 14960");
        assertTrue(AtlasTileCopies.isCoveredByShifted(OFF_GRID, edge, MAP_BLOCKS, held),
                "together the two maps hold the tile from 14912 to 15040");
    }

    @Test
    void theRingBesideTheUpperBoundTakesTheNeighborMadePastTheLowerBound() {
        assertEquals(List.of(new AtlasTileCopies.Placed<>(new TileCenter(-15104, 0), new TileCenter(14896, 0))),
                AtlasTileCopies.shifted(OFF_GRID, new TileCenter(14976, 0), 1, MAP_BLOCKS,
                        holding(new TileCenter(-15104, 0))),
                "the atlas makes it as the neighbor of the key at -14976, one map past the bound");
    }
}
