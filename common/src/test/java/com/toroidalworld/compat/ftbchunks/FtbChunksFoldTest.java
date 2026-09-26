package com.toroidalworld.compat.ftbchunks;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.toroidalworld.api.v1.TestShapes;
import com.toroidalworld.api.v1.ToroidalShape;
import com.toroidalworld.compat.AxisCopies;
import com.toroidalworld.core.CoordinateConstants;
import com.toroidalworld.core.FlatShape;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.core.WorldLoopBounds;
import com.toroidalworld.core.WorldLoopBounds.AxisBounds;

import net.minecraft.core.Direction;

import dev.ftb.mods.ftblibrary.math.XZ;

class FtbChunksFoldTest {
    private static final int[] ONE_PIECE = {0, 15};

    @Test
    void anUnboundedAxisKeepsTheCutsFtbMade() {
        int[] ftbCuts = {0, 5, 15};
        assertSame(ftbCuts, FtbChunksFold.minimapSplits(cylinder(512), Direction.Axis.X, 34, ftbCuts),
                "an unbounded axis was re-cut");
        assertSame(ftbCuts, FtbChunksFold.minimapSplits(null, Direction.Axis.X, 34, ftbCuts),
                "an unwrapped world was re-cut");
    }

    @Test
    void aWindowInsideOneRegionIsOnePiece() {
        assertArrayEquals(ONE_PIECE, FtbChunksFold.minimapSplits(torus(1024), Direction.Axis.X, 7, ONE_PIECE),
                "chunks 0..14 of a 64-chunk world sit in region 0");
    }

    @Test
    void aRegionBoundaryCutsWhereFtbWouldCutItself() {
        assertArrayEquals(new int[] {0, 5, 15}, FtbChunksFold.minimapSplits(torus(1024), Direction.Axis.X, 34, ONE_PIECE),
                "chunks 27..41 cross chunk 32, five in");
    }

    @Test
    void theSeamCutsEvenWhereTheRegionDoesNotChange() {
        assertArrayEquals(new int[] {0, 9, 15}, FtbChunksFold.minimapSplits(torus(512), Direction.Axis.X, 30, ONE_PIECE),
                "a 32-chunk world is one region, and chunk 32 wraps to 0 nine chunks in");
        assertArrayEquals(new int[] {0, 11, 15}, FtbChunksFold.minimapSplits(torus(256), Direction.Axis.X, 12, ONE_PIECE),
                "a 16-chunk world wraps at chunk 16, eleven chunks in");
    }

    @Test
    void theCopyPeriodIsTheWorldMeasuredInTilePixels() {
        assertEquals(64.0, FtbChunksFold.worldPixelPeriod(torus(512), Direction.Axis.X, 64), 1e-12,
                "512 blocks at 64 px per 512-block region is 64 px");
        assertEquals(64.0, FtbChunksFold.worldPixelPeriod(torus(256), Direction.Axis.X, 128), 1e-12,
                "256 blocks at 128 px per region is 64 px");
        assertEquals(0.0, FtbChunksFold.worldPixelPeriod(cylinder(512), Direction.Axis.X, 64), 1e-12,
                "an unbounded axis has no period");
        assertEquals(0.0, FtbChunksFold.worldPixelPeriod(null, Direction.Axis.X, 64), 1e-12,
                "an unwrapped world has no period");
    }

    @Test
    void everySeamInTheViewGetsItsPixel() {
        AxisCopies world = AxisCopies.looped(-256, 512);
        assertArrayEquals(new int[] {68, 132, 196}, FtbChunksFold.seamPixels(world, 100, 64, 68, 196),
                "block 0 at px 100 and 64 px per lap: the view 68..196 holds laps 0 and 1, outlined by three seams");
        assertArrayEquals(new int[] {68, 132}, FtbChunksFold.seamPixels(world, 100, 64, 80, 120),
                "a view inside lap 0 still gets both of its seams, off-view or not");
        assertArrayEquals(new int[0], FtbChunksFold.seamPixels(AxisCopies.UNBOUNDED, 100, 64, 68, 196),
                "an unbounded axis has no seam");
    }

    @Test
    void aSelectionInsideTheWorldKeepsItsChunks() {
        assertEquals(Set.of(XZ.of(3, 4), XZ.of(5, 6)),
                FtbChunksFold.foldedChunks(torus(1024), Set.of(XZ.of(3, 4), XZ.of(5, 6))),
                "chunks 0..63 of a 64-chunk world are already canonical");
    }

    @Test
    void aSelectionAcrossTheSeamFoldsChunkByChunk() {
        assertEquals(Set.of(XZ.of(63, 0), XZ.of(0, 0), XZ.of(63, 5)),
                FtbChunksFold.foldedChunks(torus(1024), Set.of(XZ.of(63, 0), XZ.of(64, 0), XZ.of(-1, 5))),
                "a window straddling chunk 64 wraps its far half onto 0, one chunk at a time");
    }

    @Test
    void aSelectionOnALappedClientComesBackInsideTheWorld() {
        assertEquals(Set.of(XZ.of(7, 9)),
                FtbChunksFold.foldedChunks(torus(1024), Set.of(XZ.of(7 + 64 * 3, 9 - 64 * 2))),
                "three laps out on x and two back on z is the same chunk");
    }

    @Test
    void anAxisThatDoesNotLoopKeepsItsHalfOfTheKey() {
        assertEquals(Set.of(XZ.of(9000, 1)),
                FtbChunksFold.foldedChunks(cylinder(512), Set.of(XZ.of(9000, 33))),
                "an unbounded x is carried through while z folds onto a 32-chunk world");
    }

    @Test
    void anUnwrappedWorldKeepsTheSelectionItself() {
        Set<XZ> selection = Set.of(XZ.of(9000, 33));
        assertSame(selection, FtbChunksFold.foldedChunks(null, selection), "an unwrapped world was rebuilt");
    }

    @Test
    void aViewInsideTheWorldKeepsItsScroll() {
        assertEquals(48.0, FtbChunksFold.clampScroll(AxisCopies.looped(-256, 512), 48.0, -1, 64, 32), 1e-9,
                "the 256-block view already sits on the world's centre, half a view from either edge");
    }

    @Test
    void aViewDraggedPastTheEdgeComesBackToIt() {
        assertEquals(64.0, FtbChunksFold.clampScroll(AxisCopies.looped(-256, 512), 100.0, -1, 64, 32), 1e-9,
                "a drag east puts the view centre at block 416, and the eastern edge allows 128");
        assertEquals(32.0, FtbChunksFold.clampScroll(AxisCopies.looped(-256, 512), 0.0, -1, 64, 32), 1e-9,
                "a drag west puts the view centre at block -384, and the western edge allows -128");
    }

    @Test
    void aWorldNarrowerThanTheViewIsCentred() {
        assertEquals(48.0, FtbChunksFold.clampScroll(AxisCopies.looped(-64, 128), 200.0, -1, 64, 32), 1e-9,
                "a 128-block world cannot fill a 256-block view, so the view sits on its centre, block 0");
    }

    @Test
    void anAxisThatDoesNotLoopKeepsItsScroll() {
        assertEquals(100.0, FtbChunksFold.clampScroll(AxisCopies.UNBOUNDED, 100.0, -1, 64, 32), 1e-9,
                "an unbounded axis has no edge to stop at");
    }

    private static ToroidalShape torus(int widthBlocks) {
        return shape(looped(widthBlocks), looped(widthBlocks));
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
