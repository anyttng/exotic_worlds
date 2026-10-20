package com.exoticworlds.compat.xaero;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.MapShapes;

import net.minecraft.world.level.ChunkPos;

class XaeroWriterFoldTest {
    private static final ToroidalShape OFF_GRID = MapShapes.torus(-937, 938);
    private static final ToroidalShape SMALL = MapShapes.torus(-16, 16);
    private static final int RAW = 99;

    @Test
    void aChunkPastTheSeamWritesIntoTheCanonicalTile() {
        XaeroWriterFold fold = fold(OFF_GRID);
        assertEquals(-235, fold.tileX(RAW, 938, 0), "chunk 938 folds onto -937, tile -235");
        assertEquals(5, fold.tileInRegionX(RAW, 938, 0), "tile -235 is slot 5 of its region");
        assertEquals(0, fold.tileZ(RAW, 938, 0));
    }

    @Test
    void theTileMemoFollowsTheChunk() {
        XaeroWriterFold fold = fold(OFF_GRID);
        assertEquals(-235, fold.tileX(RAW, 938, 0));
        assertEquals(1, fold.tileX(RAW, 4, 0));
        assertEquals(-235, fold.tileX(RAW, 938, 0));
    }

    @Test
    void aChunkPastALatticeSeamWritesIntoTheTileTheSkewMovesItTo() {
        XaeroWriterFold fold = fold(MapShapes.latticeTorus(-16, 16, 8));
        assertEquals(-2, fold.tileX(RAW, 0, 16), "chunk (0, 16) folds onto (-8, -16)");
        assertEquals(-4, fold.tileZ(RAW, 0, 16));
    }

    @Test
    void anUnshapedWorldKeepsXaerosOwnTile() {
        XaeroWriterFold fold = fold(null);
        assertEquals(RAW, fold.tileX(RAW, 938, 0));
        assertEquals(RAW, fold.tileZ(RAW, 938, 0));
        assertEquals(RAW, fold.tileInRegionX(RAW, 938, 0));
        assertEquals(RAW, fold.tileInRegionZ(RAW, 938, 0));
    }

    @Test
    void theLastChunkOfTheWorldTriggersTheBufferAtTheTilesLastSlot() {
        XaeroWriterFold fold = fold(OFF_GRID);
        fold.enterChunk(937, 0);
        assertEquals(1, fold.insideX(RAW));
        assertEquals(3, fold.lastInsideX(RAW), "937 is the world's last chunk, so the tile is complete after it");
        assertEquals(0, fold.lastInsideZ(RAW));
    }

    @Test
    void aChunkPastTheSeamTakesItsCanonicalSlotInsideTheTile() {
        XaeroWriterFold fold = fold(OFF_GRID);
        fold.enterChunk(938, 0);
        assertEquals(3, fold.insideX(RAW), "938 folds onto -937, slot 3 of tile -235");
        assertEquals(3, fold.lastInsideX(RAW));
    }

    @Test
    void anUnshapedWorldKeepsXaerosOwnSlots() {
        XaeroWriterFold fold = fold(null);
        fold.enterChunk(938, 0);
        assertEquals(RAW, fold.insideX(RAW));
        assertEquals(RAW, fold.insideZ(RAW));
        assertEquals(RAW, fold.lastInsideX(RAW));
        assertEquals(RAW, fold.lastInsideZ(RAW));
    }

    @Test
    void aFrameAcrossTheSeamVisitsTheCanonicalRegionsItMeets() {
        XaeroWriterFold fold = fold(SMALL);
        assertEquals(0, fold.visitFromRegionX(RAW, 3, 0, 4, 0));
        assertEquals(0, fold.visitFromRegionZ(RAW));
        assertEquals(0, fold.visitToRegionX(RAW));
        int last = fold.visitToRegionZ(RAW);
        Set<ChunkPos> visited = new HashSet<>();
        for (int index = 0; index <= last; index++) {
            visited.add(fold.visitedRegion(0, index));
        }

        assertEquals(Set.of(new ChunkPos(0, 0), new ChunkPos(-1, 0)), visited,
                "blocks 192..255 stay in region 0, 256..319 fold onto -256..-193 in region -1");
    }

    @Test
    void anUnshapedWorldVisitsXaerosOwnRegions() {
        XaeroWriterFold fold = fold(null);
        assertEquals(RAW, fold.visitFromRegionX(RAW, 3, 0, 4, 0));
        assertEquals(RAW, fold.visitFromRegionZ(RAW));
        assertEquals(RAW, fold.visitToRegionX(RAW));
        assertEquals(RAW, fold.visitToRegionZ(RAW));
        assertEquals(new ChunkPos(7, -3), fold.visitedRegion(7, -3));
    }

    @Test
    void aComparisonPastTheSeamFoldsByItsCenterChunk() {
        XaeroWriterFold fold = fold(OFF_GRID);
        assertEquals(new ChunkPos(-953, 0), fold.comparison(922, 0), "922 + 16 = 938 folds onto -937");
        assertEquals(new ChunkPos(-953, 0), fold.comparisonLeaf(922, 0, 1, 2));
    }

    @Test
    void anUnshapedWorldKeepsXaerosOwnComparison() {
        XaeroWriterFold fold = fold(null);
        assertEquals(new ChunkPos(922, 0), fold.comparison(922, 0));
        assertEquals(new ChunkPos(1, 2), fold.comparisonLeaf(922, 0, 1, 2));
    }

    private static XaeroWriterFold fold(@Nullable ToroidalShape shape) {
        XaeroWriterFold fold = new XaeroWriterFold();
        fold.beginRender(shape);
        return fold;
    }
}
