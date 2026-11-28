package com.exoticworlds.compat.mapatlases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import com.exoticworlds.compat.map.AtlasTileCopies;
import com.exoticworlds.compat.map.AtlasTileCopies.TileCenter;
import com.exoticworlds.compat.map.MapFolds;
import com.exoticworlds.core.fold.WorldFold;

import net.minecraft.core.BlockPos;

class MapGridKeyCopiesTest {
    private static final int MAP_BLOCKS = 128;
    private static final WorldFold SMALL = MapFolds.torus(-32, 32);
    private static final WorldFold OFF_GRID = MapFolds.torus(-937, 938);

    @Test
    void aKeyPastTheUpperBoundFoldsOntoItsCopyInside() {
        assertEquals(new TileCenter(-512, 0), MapGridKeyCopies.foldCenter(SMALL, 512, 0, MAP_BLOCKS),
                "the active key beside the upper bound sits on the bound and folds a lap back");
        assertEquals(new TileCenter(-384, 0), MapGridKeyCopies.foldCenter(SMALL, 640, 0, MAP_BLOCKS),
                "its neighbor past the bound folds onto the tile beside the lower bound");
        assertEquals(new TileCenter(384, 0), MapGridKeyCopies.foldCenter(SMALL, -640, 0, MAP_BLOCKS),
                "a neighbor past the lower bound folds onto the tile beside the upper bound");
        assertEquals(new TileCenter(384, 128), MapGridKeyCopies.foldCenter(SMALL, 384, 128, MAP_BLOCKS),
                "a key inside the bounds stays");
    }

    @Test
    void aSkewedSeamFoldsTheKeyMovedSideways() {
        WorldFold shape = MapFolds.latticeTorus(-16, 16, 8);
        BlockPos canonical = shape.fold(new BlockPos(0, 0, 512));
        assertEquals(new TileCenter(canonical.getX(), canonical.getZ()),
                MapGridKeyCopies.foldCenter(shape, 0, 512, MAP_BLOCKS), "a lap in Z takes the skew along X");
    }

    @Test
    void aWidthOffTheMapGridLeavesTheKeyPastTheBound() {
        assertEquals(new TileCenter(15104, 0), MapGridKeyCopies.foldCenter(OFF_GRID, 15104, 0, MAP_BLOCKS),
                "30000 blocks are no whole number of maps, so no copy lands on the grid");
    }

    @Test
    void noShapeLeavesTheKeyAsItIs() {
        assertEquals(new TileCenter(15104, 0), MapGridKeyCopies.foldCenter(null, 15104, 0, MAP_BLOCKS),
                "an unwrapped level has no copies");
        assertNull(MapGridKeyCopies.shiftedUnder(null, new TileCenter(-14976, 0), -14984.5, 0.5, MAP_BLOCKS,
                holding(new TileCenter(14976, 0))), "and no moved copies");
    }

    @Test
    void thePlayerFindsTheMapMovedUnderThemOffTheGrid() {
        AtlasTileCopies.TileLookup<TileCenter> held = holding(new TileCenter(14976, 0));
        assertEquals(new TileCenter(14976, 0),
                MapGridKeyCopies.shiftedUnder(OFF_GRID, new TileCenter(-14976, 0), -14984.5, 0.5, MAP_BLOCKS, held),
                "beside the lower bound the map past the upper bound lies under the player a lap back");
        assertNull(
                MapGridKeyCopies.shiftedUnder(OFF_GRID, new TileCenter(-14976, 0), -14940.5, 0.5, MAP_BLOCKS, held),
                "a point of the tile its moved copy leaves out has no map under it");
    }

    @Test
    void aWholeMapCopyIsNoMovedCopy() {
        assertNull(MapGridKeyCopies.shiftedUnder(SMALL, new TileCenter(-512, 0), -504.5, 0.5, MAP_BLOCKS,
                holding(new TileCenter(512, 0))), "a whole-map copy is asked as a key, not as a moved copy");
    }

    private static AtlasTileCopies.TileLookup<TileCenter> holding(TileCenter... centers) {
        Set<TileCenter> held = Set.of(centers);
        return (x, z) -> lookup(held, x, z);
    }

    private static @Nullable TileCenter lookup(Set<TileCenter> held, int x, int z) {
        TileCenter center = new TileCenter(x, z);
        return held.contains(center) ? center : null;
    }
}
