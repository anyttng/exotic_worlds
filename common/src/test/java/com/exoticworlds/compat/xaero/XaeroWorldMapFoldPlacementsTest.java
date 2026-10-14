package com.exoticworlds.compat.xaero;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.MapShapes;
import com.exoticworlds.compat.WorldCopies;
import com.exoticworlds.compat.xaero.XaeroWorldMapFold.TilePlacement;

import net.minecraft.world.level.ChunkPos;

class XaeroWorldMapFoldPlacementsTest {
    private static final int SLOT = 64;
    private static final ToroidalShape OFF_GRID_TORUS = MapShapes.torus(-937, 938);
    private static final List<WorldCopies.Copy> IDENTITY_ONLY = List.of(WorldCopies.IDENTITY);

    @Test
    void aFrameInsideTheWorldKeepsTheExportersOwnWalk() {
        assertNull(XaeroWorldMapFold.frameRegions(MapShapes.torus(-32, 32), -8, -8, 7, 7),
                "tiles -8..7 cover -512..511, the whole world and nothing past it");
    }

    @Test
    void aFramePastTheSeamWalksTheCanonicalRegionsItsCopiesMeet() {
        assertArrayEquals(new long[] {ChunkPos.asLong(-1, 0)},
                XaeroWorldMapFold.frameRegions(MapShapes.torus(-16, 16), 4, 0, 5, 0),
                "tiles 4..5 cover 256..383, one lap past the seam at 256, so they fold onto -256..-129 in region -1");
    }

    @Test
    void anUnshapedFrameKeepsTheExportersOwnWalk() {
        assertNull(XaeroWorldMapFold.frameRegions(null, 100, 100, 120, 120));
    }

    @Test
    void aTileInsideTheWorldIsPlacedWholeOnEveryCopy() {
        assertEquals(List.of(new TilePlacement(0, 0, SLOT, SLOT, 0.0F, 0.0F, 1.0F, 1.0F),
                        new TilePlacement(-30000, 0, SLOT, SLOT, 0.0F, 0.0F, 1.0F, 1.0F)),
                XaeroWorldMapFold.placements(OFF_GRID_TORUS, 0, 0, SLOT,
                        List.of(WorldCopies.IDENTITY, new WorldCopies.Copy(-30000, 0))));
    }

    @Test
    void aTileOnAnOffGridSeamIsClippedToTheWorld() {
        assertEquals(List.of(new TilePlacement(14976, 0, 32, SLOT, 0.0F, 0.0F, 0.5F, 1.0F)),
                XaeroWorldMapFold.placements(OFF_GRID_TORUS, 14976, 0, SLOT, IDENTITY_ONLY),
                "the world ends at 15008, half way into the tile at 14976");
    }

    @Test
    void aTilePastTheWorldIsNeverPlaced() {
        assertEquals(List.of(), XaeroWorldMapFold.placements(OFF_GRID_TORUS, 15040, 0, SLOT, IDENTITY_ONLY));
    }

    @Test
    void anUnshapedTileIsPlacedWhole() {
        assertEquals(List.of(new TilePlacement(64, -128, SLOT, SLOT, 0.0F, 0.0F, 1.0F, 1.0F)),
                XaeroWorldMapFold.placements(null, 64, -128, SLOT, IDENTITY_ONLY));
    }

    @Test
    void aPlacementMeetsOnlyASpanItOverlaps() {
        TilePlacement placement = new TilePlacement(0, 0, 32, SLOT, 0.0F, 0.0F, 0.5F, 1.0F);
        assertTrue(placement.meets(31, 0, 64, 64), "block 31 is the placement's last column");
        assertFalse(placement.meets(32, 0, 64, 64), "block 32 is the first column past it");
    }
}
