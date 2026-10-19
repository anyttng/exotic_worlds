package com.exoticworlds.compat.xaero;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.MapShapes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

class XaeroWorldMapFoldRegionsTest {
    private static final ToroidalShape OFF_GRID = MapShapes.torus(-937, 938);
    private static final ToroidalShape SMALL = MapShapes.torus(-16, 16);
    private static final int REACH = 6;

    @Test
    void aRegionWiderThanAHalfShiftedWorldMeetsFourCanonicalRegions() {
        assertArrayEquals(corners(0, 0, -1, 0, 0, -1, -1, -1), XaeroWorldMapFold.canonicalRegionCorners(SMALL, 0, 0, 0),
                "blocks 0..511 of a 512-block world centered on 0 reach 256..511, which fold onto -256..-1");
    }

    @Test
    void aRegionInsideTheWorldIsItsOwnCanonicalRegion() {
        assertArrayEquals(corners(0, 0, 0, 0, 0, 0, 0, 0), XaeroWorldMapFold.canonicalRegionCorners(OFF_GRID, 0, 0, 0));
    }

    @Test
    void aRegionPastAnOffGridSeamFoldsOntoTwoRegionsAcross() {
        assertArrayEquals(corners(-29, 0, -28, 0, -29, 0, -28, 0),
                XaeroWorldMapFold.canonicalRegionCorners(OFF_GRID, 30, 0, 0),
                "15360..15871 fold onto -14640..-14129, across the region line at -14336");
    }

    @Test
    void aLeveledRegionFoldsByItsOwnSide() {
        assertArrayEquals(corners(14, 0, -15, 0, 14, 0, -15, 0),
                XaeroWorldMapFold.canonicalRegionCorners(OFF_GRID, 14, 0, 1),
                "level 1 regions are 1024 blocks: 14336..15359, whose high end folds onto -14641");
    }

    @Test
    void anUnshapedWorldKeepsTheRawRegion() {
        assertArrayEquals(corners(5, -2, 5, -2, 5, -2, 5, -2), XaeroWorldMapFold.canonicalRegionCorners(null, 5, -2, 0));
    }

    @Test
    void theRegionFoldIsTheFoldOfTheRegionsOriginBlock() {
        for (ToroidalShape shape : List.of(OFF_GRID, SMALL, MapShapes.latticeTorus(-16, 16, 8),
                MapShapes.cylinder(-16, 16))) {
            for (int regX = -REACH; regX <= REACH; regX++) {
                for (int regZ = -REACH; regZ <= REACH; regZ++) {
                    BlockPos origin = XaeroWorldMapFold.foldBlock(shape, regX * XaeroWorldMapFold.REGION_BLOCKS,
                            regZ * XaeroWorldMapFold.REGION_BLOCKS);
                    ChunkPos expected = new ChunkPos(Math.floorDiv(origin.getX(), XaeroWorldMapFold.REGION_BLOCKS),
                            Math.floorDiv(origin.getZ(), XaeroWorldMapFold.REGION_BLOCKS));
                    assertEquals(expected, XaeroWorldMapFold.foldRegion(shape, regX, regZ),
                            "region " + regX + ", " + regZ + " of " + shape);
                }
            }
        }
    }

    private static long[] corners(int... coordinates) {
        long[] packed = new long[coordinates.length / 2];
        for (int i = 0; i < packed.length; i++) {
            packed[i] = ChunkPos.pack(coordinates[2 * i], coordinates[2 * i + 1]);
        }

        return packed;
    }
}
