package com.exoticworlds.compat.xaero;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.exoticworlds.compat.MapShapes;

import net.minecraft.world.level.ChunkPos;

class XaeroWorldMapFoldSeamTest {
    @Test
    void aRegionInsideAnAlignedWorldFansOutNothing() {
        assertArrayEquals(new long[0], XaeroWorldMapFold.fannedRegions(MapShapes.torus(-32, 32), -1, 0, 0),
                "region -1 covers -512..-1, the world's first half");
        assertArrayEquals(new long[0], XaeroWorldMapFold.fannedRegions(MapShapes.torus(-32, 32), 0, -1, 0),
                "region 0 covers 0..511, the world's second half");
    }

    @Test
    void aRegionPastAnAlignedSeamFansOutItsCanonicalRegion() {
        assertArrayEquals(new long[] {ChunkPos.asLong(-1, 0)},
                XaeroWorldMapFold.fannedRegions(MapShapes.torus(-32, 32), 1, 0, 0),
                "region 1 covers 512..1023, one lap past the seam at 512");
    }

    @Test
    void aRegionOfAHalfShiftedWorldFansOutEveryRegionItsFoldedSpanTouches() {
        assertArrayEquals(
                sorted(ChunkPos.asLong(-1, -1), ChunkPos.asLong(-1, 0), ChunkPos.asLong(0, -1), ChunkPos.asLong(0, 0)),
                sorted(XaeroWorldMapFold.fannedRegions(MapShapes.torus(-16, 16), 0, 0, 0)),
                "region 0 spans 0..511 against a world of -256..255 on both axes");
    }

    @Test
    void aBranchRegionFansOutItsLeavesRegionByRegion() {
        assertArrayEquals(
                new long[] {ChunkPos.asLong(-2, 0), ChunkPos.asLong(-2, 1), ChunkPos.asLong(-1, 0), ChunkPos.asLong(-1, 1)},
                XaeroWorldMapFold.fannedLeaves(MapShapes.torus(-64, 64), 1, 0, 1),
                "level-1 region 1 covers 1024..2047, one lap past the seam, so it fans out level-1 region -1");
    }

    @Test
    void anUnshapedWorldFansOutNothing() {
        assertArrayEquals(new long[0], XaeroWorldMapFold.fannedRegions(null, 7, 0, 0), "an unshaped world has no seam");
    }

    private static long[] sorted(long... regions) {
        long[] copy = regions.clone();
        Arrays.sort(copy);
        return copy;
    }
}
