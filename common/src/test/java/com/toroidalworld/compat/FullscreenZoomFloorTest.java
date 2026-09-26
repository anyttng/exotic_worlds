package com.toroidalworld.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.toroidalworld.api.v1.TestShapes;
import com.toroidalworld.api.v1.ToroidalShape;
import com.toroidalworld.core.CoordinateConstants;
import com.toroidalworld.core.FlatShape;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.core.WorldLoopBounds;
import com.toroidalworld.core.WorldLoopBounds.AxisBounds;

class FullscreenZoomFloorTest {
    @Test
    void journeyMapKeepsTheWorldAtLeast64PixelsWide() {
        assertEquals(64, FullscreenZoomFloor.journeyMapZoom(512), "a 512-block world: 64 * 512 / 512 = 64 px per region");
        assertEquals(32, FullscreenZoomFloor.journeyMapZoom(1024), "a 1024-block world: 64 * 512 / 1024 = 32");
        assertEquals(256, FullscreenZoomFloor.journeyMapZoom(128), "a 128-block world: 64 * 512 / 128 = 256");
        assertEquals(8, FullscreenZoomFloor.journeyMapZoom(4096), "a 4096-block world: 64 * 512 / 4096 = 8");
        assertEquals(110, FullscreenZoomFloor.journeyMapZoom(300), "a 300-block world: ceil(32768 / 300) = 110");
    }

    @Test
    void xaeroKeepsTheWorldAtLeast64PixelsWide() {
        assertEquals(0.125, FullscreenZoomFloor.xaeroScale(512, 1.0), 1e-12, "a 512-block world at multiplier 1: 64 / 512");
        assertEquals(64.0 / (512 * 1.2676), FullscreenZoomFloor.xaeroScale(512, 1.2676), 1e-12,
                "a 512-block world on a 1369-px screen: 64 / (512 * 1369 / 1080)");
        assertEquals(0.5, FullscreenZoomFloor.xaeroScale(128, 1.0), 1e-12, "a 128-block world at multiplier 1: 64 / 128");
    }

    @Test
    void ftbChunksKeepsTheWorldAtLeast64PixelsWide() {
        assertEquals(32, FullscreenZoomFloor.ftbChunksZoom(512), "a 512-block world: 512 * 32 / 256 = 64 px");
        assertEquals(16, FullscreenZoomFloor.ftbChunksZoom(1024), "a 1024-block world: 64 * 256 / 1024 = 16");
        assertEquals(128, FullscreenZoomFloor.ftbChunksZoom(128), "a 128-block world: 64 * 256 / 128 = 128");
        assertEquals(55, FullscreenZoomFloor.ftbChunksZoom(300), "a 300-block world: ceil(16384 / 300) = 55");
    }

    @Test
    void ftbChunksTakesTheNarrowestLoopedAxis() {
        assertEquals(32, FullscreenZoomFloor.ftbChunksZoom(torus(1024, 512)), "the 512-block axis sets the floor");
        assertEquals(32, FullscreenZoomFloor.ftbChunksZoom(cylinder(512)), "an unbounded axis asks for no floor");
    }

    @Test
    void journeyMapCoversTheWindowExactly() {
        assertEquals(2560, FullscreenZoomFloor.journeyMapCoverZoom(512, 2560),
                "a 512-block world over 2560 px needs 2560 px per region, not the next power of two");
        assertEquals(86, FullscreenZoomFloor.journeyMapCoverZoom(8192, 1369), "ceil(1369 * 512 / 8192) is 86");
        assertEquals(16384, FullscreenZoomFloor.journeyMapCoverZoom(64, 2560),
                "a floor past JourneyMap's deepest level is not held at 16384");
    }

    @Test
    void ftbChunksCoversTheWindow() {
        assertEquals(685, FullscreenZoomFloor.ftbChunksCoverZoom(512, 1369), "ceil(1369 * 256 / 512) is 685");
        assertEquals(43, FullscreenZoomFloor.ftbChunksCoverZoom(8192, 1369), "ceil(1369 * 256 / 8192) is 43");
        assertEquals(1024, FullscreenZoomFloor.ftbChunksCoverZoom(64, 2560),
                "a floor past FTB Chunks' deepest zoom is not held at 1024");
    }

    @Test
    void ftbChunksReadsEachLoopedAxisAgainstItsOwnWindowSide() {
        assertEquals(685, FullscreenZoomFloor.ftbChunksCoverZoom(torus(1024, 512), 2560, 1369),
                "X of 1024 over 2560 px needs 640 and Z of 512 over 1369 px needs 685, the larger holds");
        assertEquals(86, FullscreenZoomFloor.ftbChunksCoverZoom(cylinder(4096), 2560, 1369),
                "a cylinder reads its Z width against the window height alone: ceil(1369 * 256 / 4096) = 86");
    }

    @Test
    void journeyMapSeatsAStoredLevelBelowTheFloor() {
        assertEquals(64, FullscreenZoomFloor.journeyMapSeatedZoom(8, 64, MapCopies.REPEATED),
                "a level of 8 under a floor of 64 is lifted onto the floor");
        assertEquals(128, FullscreenZoomFloor.journeyMapSeatedZoom(8, 110, MapCopies.REPEATED),
                "repeated copies keep the power-of-two levels: a floor of 110 seats at 128");
        assertEquals(2560, FullscreenZoomFloor.journeyMapSeatedZoom(512, 2560, MapCopies.SINGLE),
                "one copy seats at the exact cover floor");
        assertEquals(256, FullscreenZoomFloor.journeyMapSeatedZoom(256, 64, MapCopies.REPEATED),
                "a level at or above the floor is left alone");
        assertEquals(512, FullscreenZoomFloor.journeyMapSeatedZoom(512, 0, MapCopies.SINGLE),
                "no looped world, no floor");
    }

    @Test
    void xaeroCoversTheWindow() {
        assertEquals(5.0, FullscreenZoomFloor.xaeroCoverScale(512, 1.0, 2560), 1e-12, "2560 px over 512 blocks at multiplier 1");
        assertEquals(1369 / (512 * 1.2676), FullscreenZoomFloor.xaeroCoverScale(512, 1.2676, 1369), 1e-12,
                "the multiplier does not divide the cover scale");
    }

    @Test
    void theCoverFloorReadsEachLoopedAxisAgainstItsOwnWindowSide() {
        assertEquals(1369, FullscreenZoomFloor.journeyMapCoverZoom(torus(1024, 512), 2560, 1369),
                "X of 1024 over 2560 px needs 1280 and Z of 512 over 1369 px needs 1369, the larger holds");
        assertEquals(172, FullscreenZoomFloor.journeyMapCoverZoom(cylinder(4096), 2560, 1369),
                "a cylinder reads its Z width against the window height alone: ceil(1369 * 512 / 4096) = 172");
        assertEquals(5.0, FullscreenZoomFloor.xaeroCoverScale(torus(512, 1024), 1.0, 2560, 1369), 1e-12,
                "X of 512 over 2560 px outweighs Z of 1024 over 1369 px");
    }

    @Test
    void journeyMapTakesTheNarrowestLoopedAxis() {
        assertEquals(64, FullscreenZoomFloor.journeyMapZoom(torus(1024, 512)), "the 512-block axis sets the floor");
        assertEquals(64, FullscreenZoomFloor.journeyMapZoom(cylinder(512)), "an unbounded axis asks for no floor");
    }

    @Test
    void xaeroTakesTheNarrowestLoopedAxis() {
        assertEquals(0.125, FullscreenZoomFloor.xaeroScale(torus(1024, 512), 1.0), 1e-12,
                "the 512-block axis sets the floor");
        assertEquals(0.125, FullscreenZoomFloor.xaeroScale(cylinder(512), 1.0), 1e-12,
                "an unbounded axis asks for no floor");
    }

    private static ToroidalShape torus(int widthBlocksX, int widthBlocksZ) {
        return shape(looped(widthBlocksX), looped(widthBlocksZ));
    }

    private static ToroidalShape cylinder(int widthBlocksZ) {
        return shape(AxisBounds.Unbounded.INSTANCE, looped(widthBlocksZ));
    }

    private static ToroidalShape shape(AxisBounds x, AxisBounds z) {
        return TestShapes.of(WorldFolds.of(FlatShape.torus(new WorldLoopBounds(x, z))));
    }

    private static AxisBounds looped(int widthBlocks) {
        return new AxisBounds.Looped(0, widthBlocks / CoordinateConstants.CHUNK_WIDTH);
    }
}
