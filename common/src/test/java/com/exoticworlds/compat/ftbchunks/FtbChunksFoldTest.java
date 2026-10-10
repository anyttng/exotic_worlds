package com.exoticworlds.compat.ftbchunks;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.TestShapes;
import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.AxisCopies;
import com.exoticworlds.compat.MapCopies;
import com.exoticworlds.compat.MapShapes;
import com.exoticworlds.compat.WorldCopies;
import com.exoticworlds.core.CoordinateConstants;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;

import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;

import dev.ftb.mods.ftblibrary.math.XZ;

class FtbChunksFoldTest {
    private static final int[] ONE_PIECE = {0, 15};
    private static final int ROWS_INSIDE = 7;
    private static final int SKEW_CHUNKS = 8;
    private static final int SKEW = SKEW_CHUNKS * CoordinateConstants.CHUNK_WIDTH;
    private static final int WIDTH = 512;

    private static final ToroidalShape LATTICE = MapShapes.latticeTorus(0, 32, SKEW_CHUNKS);
    private static final ToroidalShape CENTRED_LATTICE = MapShapes.latticeTorus(-16, 16, SKEW_CHUNKS);
    private static final ToroidalShape CENTRED_TORUS = MapShapes.torus(-16, 16);

    @Test
    void anUnboundedAxisKeepsTheCutsFtbMade() {
        int[] ftbCuts = {0, 5, 15};
        assertSame(ftbCuts, FtbChunksFold.minimapSplits(cylinder(512), Direction.Axis.X, 34, ROWS_INSIDE, ftbCuts),
                "an unbounded axis was re-cut");
        assertSame(ftbCuts, FtbChunksFold.minimapSplits(null, Direction.Axis.X, 34, ROWS_INSIDE, ftbCuts),
                "an unwrapped world was re-cut");
    }

    @Test
    void aWindowInsideOneRegionIsOnePiece() {
        assertArrayEquals(ONE_PIECE,
                FtbChunksFold.minimapSplits(torus(1024), Direction.Axis.X, 7, ROWS_INSIDE, ONE_PIECE),
                "chunks 0..14 of a 64-chunk world sit in region 0");
    }

    @Test
    void aRegionBoundaryCutsWhereFtbWouldCutItself() {
        assertArrayEquals(new int[] {0, 5, 15},
                FtbChunksFold.minimapSplits(torus(1024), Direction.Axis.X, 34, ROWS_INSIDE, ONE_PIECE),
                "chunks 27..41 cross chunk 32, five in");
    }

    @Test
    void theSeamCutsEvenWhereTheRegionDoesNotChange() {
        assertArrayEquals(new int[] {0, 9, 15},
                FtbChunksFold.minimapSplits(torus(512), Direction.Axis.X, 30, ROWS_INSIDE, ONE_PIECE),
                "a 32-chunk world is one region, and chunk 32 wraps to 0 nine chunks in");
        assertArrayEquals(new int[] {0, 11, 15},
                FtbChunksFold.minimapSplits(torus(256), Direction.Axis.X, 12, ROWS_INSIDE, ONE_PIECE),
                "a 16-chunk world wraps at chunk 16, eleven chunks in");
    }

    @Test
    void theRowsPastASkewedSeamCutWhereTheirMovedRunWraps() {
        assertArrayEquals(new int[] {0, 3, 15},
                FtbChunksFold.minimapSplits(LATTICE, Direction.Axis.X, 12, 31, ONE_PIECE),
                "chunks 5..19 of the rows past chunk 32 sit 8 chunks west, so chunk 8 wraps to 0 three in");
        assertArrayEquals(ONE_PIECE,
                FtbChunksFold.minimapSplits(torus(512), Direction.Axis.X, 12, 31, ONE_PIECE),
                "a plain torus cut chunks 5..19 although no row of them wraps");
    }

    @Test
    void everyPieceOfALappedWindowIsOneRunInOneRegion() {
        int centreX = 24;
        int centreZ = -18;
        int[] cutsX = FtbChunksFold.minimapSplits(CENTRED_LATTICE, Direction.Axis.X, centreX, centreZ,
                ftbRegionSplits(centreX));
        int[] cutsZ = FtbChunksFold.minimapSplits(CENTRED_LATTICE, Direction.Axis.Z, centreX, centreZ,
                ftbRegionSplits(centreZ));
        for (int ix = 0; ix < cutsX.length - 1; ix++) {
            for (int iz = 0; iz < cutsZ.length - 1; iz++) {
                ChunkPos origin = CENTRED_LATTICE.fold(new ChunkPos(centreX + cutsX[ix] - 7, centreZ + cutsZ[iz] - 7));
                for (int x = cutsX[ix]; x < cutsX[ix + 1]; x++) {
                    for (int z = cutsZ[iz]; z < cutsZ[iz + 1]; z++) {
                        ChunkPos folded = CENTRED_LATTICE.fold(new ChunkPos(centreX + x - 7, centreZ + z - 7));
                        assertEquals(new ChunkPos(origin.x() + x - cutsX[ix], origin.z() + z - cutsZ[iz]), folded,
                                "window chunk (" + x + ", " + z + ") is not one run from its piece's origin");
                        assertEquals(Math.floorDiv(origin.x(), 32), Math.floorDiv(folded.x(), 32),
                                "window chunk (" + x + ", " + z + ") left its piece's region along X");
                        assertEquals(Math.floorDiv(origin.z(), 32), Math.floorDiv(folded.z(), 32),
                                "window chunk (" + x + ", " + z + ") left its piece's region along Z");
                    }
                }
            }
        }
    }

    @Test
    void theSkewedSeamCutsTheColumns() {
        assertArrayEquals(new int[] {0, 8, 15},
                FtbChunksFold.minimapSplits(LATTICE, Direction.Axis.Z, 12, 31, ONE_PIECE),
                "rows 24..38 cross the Z seam at chunk 32, eight in");
    }

    @Test
    void aChunkPastASkewedSeamFoldsMovedSideways() {
        assertEquals(new ChunkPos(27, 0), FtbChunksFold.foldedChunk(LATTICE, 3, 32),
                "chunk 32 rows up is the world's row 0 moved 8 chunks west, so chunk 3 lands on 27");
        assertEquals(Set.of(XZ.of(27, 0)), FtbChunksFold.foldedChunks(LATTICE, Set.of(XZ.of(3, 32))),
                "a claim past the skewed seam was folded one axis at a time");
    }

    @Test
    void aRepeatedMapDrawsEveryCopyTheViewMeets() {
        assertEquals(Set.of(WorldCopies.IDENTITY, new WorldCopies.Copy(0, WIDTH), new WorldCopies.Copy(WIDTH, 0),
                new WorldCopies.Copy(WIDTH, WIDTH)),
                new HashSet<>(FtbChunksFold.drawnCopies(CENTRED_TORUS, 0, 0, 300, 300, MapCopies.REPEATED)),
                "a view over the north-east corner of a 512-block torus does not meet the four copies there");
    }

    @Test
    void theRowPastASkewedSeamIsDrawnMovedSideways() {
        assertEquals(Set.of(WorldCopies.IDENTITY, new WorldCopies.Copy(SKEW, WIDTH),
                new WorldCopies.Copy(SKEW - WIDTH, WIDTH)),
                new HashSet<>(FtbChunksFold.drawnCopies(CENTRED_LATTICE, -256, 200, 256, 400, MapCopies.REPEATED)),
                "the row above the world is not moved 128 blocks east");
    }

    @Test
    void aSingleCopyMapDrawsTheCanonicalCopyAlone() {
        assertEquals(List.of(WorldCopies.IDENTITY),
                FtbChunksFold.drawnCopies(CENTRED_LATTICE, -1500, -1500, 1500, 1500, MapCopies.SINGLE),
                "the copies beside the world were drawn under SINGLE");
        assertEquals(List.of(),
                FtbChunksFold.drawnCopies(CENTRED_LATTICE, 3000, 3000, 3100, 3100, MapCopies.SINGLE),
                "a view past the world drew a copy under SINGLE");
    }

    @Test
    void anUnwrappedWorldDrawsItselfInBothModes() {
        assertEquals(List.of(WorldCopies.IDENTITY),
                FtbChunksFold.drawnCopies(null, -40000000, -40000000, 40000000, 40000000, MapCopies.SINGLE),
                "an unwrapped world lost itself under SINGLE");
        assertEquals(List.of(WorldCopies.IDENTITY),
                FtbChunksFold.drawnCopies(null, -40000000, -40000000, 40000000, 40000000, MapCopies.REPEATED),
                "an unwrapped world grew copies under REPEATED");
    }

    @Test
    void theXSeamPastASkewedZSeamMovesWithItsRow() {
        FtbChunksFold.SeamView view = new FtbChunksFold.SeamView(0, 0, FtbChunksFold.REGION_BLOCKS, -256, 200, 512,
                200);
        assertEquals(Set.of(new FtbChunksFold.SeamLine(true, -256, 200, 256),
                new FtbChunksFold.SeamLine(true, -256 + SKEW, 256, 400)),
                verticalLines(FtbChunksFold.seamLines(CENTRED_LATTICE, view)),
                "the X seam of the row above the world is not 128 blocks east of the world's own");
        assertEquals(Set.of(new FtbChunksFold.SeamLine(true, -256, 200, 256),
                new FtbChunksFold.SeamLine(true, -256, 256, 400)),
                verticalLines(FtbChunksFold.seamLines(CENTRED_TORUS, view)),
                "a plain torus moved its X seam between rows");
    }

    @Test
    void anUnwrappedWorldDrawsNoSeam() {
        assertEquals(List.of(), FtbChunksFold.seamLines(null,
                new FtbChunksFold.SeamView(0, 0, FtbChunksFold.REGION_BLOCKS, -256, 200, 512, 200)),
                "an unwrapped world drew a seam");
    }

    @Test
    void theHaloPastASkewedSeamTakesTheMovedColumn() {
        List<FtbChunksFold.HaloPixel> halo = FtbChunksFold.haloPixels(LATTICE, 2, 31);
        assertEquals(16, halo.size(), "the north edge row of chunk (2, 31) is not 16 halo pixels");
        assertTrue(halo.contains(new FtbChunksFold.HaloPixel(32, 511, 32 + WIDTH - SKEW, -1)),
                "block 32 of the world's last row does not land 128 blocks west, wrapped, past the first row");
        assertFalse(halo.contains(new FtbChunksFold.HaloPixel(32, 511, 32, -1)),
                "the halo row past the skewed seam took the unmoved column");
    }

    @Test
    void theHaloOfAPlainTorusMirrorsStraightAcross() {
        List<FtbChunksFold.HaloPixel> halo = FtbChunksFold.haloPixels(torus(512), 0, 0);
        assertTrue(halo.contains(new FtbChunksFold.HaloPixel(0, 5, WIDTH, 5)),
                "the west edge column was not mirrored one block past the east edge");
        assertTrue(halo.contains(new FtbChunksFold.HaloPixel(5, 0, 5, WIDTH)),
                "the north edge row was not mirrored one block past the south edge");
        assertTrue(halo.contains(new FtbChunksFold.HaloPixel(0, 0, WIDTH, WIDTH)),
                "the corner was not mirrored past the opposite corner");
    }

    @Test
    void aChunkOffTheEdgesHasNoHalo() {
        assertEquals(List.of(), FtbChunksFold.haloPixels(LATTICE, 5, 5), "an inner chunk wrote a halo");
        assertEquals(List.of(), FtbChunksFold.haloPixels(null, 0, 0), "an unwrapped world wrote a halo");
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

    private static int[] ftbRegionSplits(int centreChunk) {
        int firstRegion = (centreChunk - 7) >> 5;
        for (int m = 1; m < 15; m++) {
            if ((centreChunk + m - 7) >> 5 != firstRegion) {
                return new int[] {0, m, 15};
            }
        }

        return ONE_PIECE;
    }

    private static Set<FtbChunksFold.SeamLine> verticalLines(List<FtbChunksFold.SeamLine> lines) {
        Set<FtbChunksFold.SeamLine> vertical = new HashSet<>();
        for (FtbChunksFold.SeamLine line : lines) {
            if (line.vertical()) {
                vertical.add(line);
            }
        }

        return vertical;
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
