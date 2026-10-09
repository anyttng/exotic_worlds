package com.exoticworlds.compat.xaero;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.MapShapes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

class XaeroWorldMapFoldSeatTest {
    private static final ToroidalShape LATTICE = MapShapes.latticeTorus(-16, 16, 16);
    private static final ToroidalShape HALF_SKEW_LATTICE = MapShapes.latticeTorus(-16, 16, 8);
    private static final ToroidalShape TORUS = MapShapes.torus(-16, 16);
    private static final ToroidalShape CYLINDER = MapShapes.cylinder(-16, 16);

    @Test
    void aFootprintPastTheZSeamOfALatticeTorusFoldsWhole() {
        assertEquals(new Vec3(-155.5, 0, -249.5), XaeroWorldMapFold.foldPoint(LATTICE, 100.5, 262.5),
                "crossing the Z seam moves X by 256 blocks, so a per-axis fold would have kept it at 100.5");
        assertEquals(new Vec3(100.5, 0, -249.5), XaeroWorldMapFold.foldPoint(TORUS, 100.5, 262.5));
        assertEquals(new Vec3(100.5, 0, 262.5), XaeroWorldMapFold.foldPoint(CYLINDER, 100.5, 262.5));
        assertEquals(new Vec3(100.5, 0, 262.5), XaeroWorldMapFold.foldPoint(null, 100.5, 262.5));
    }

    @Test
    void theCursorPastTheZSeamOfALatticeTorusNamesTheCanonicalBlock() {
        assertEquals(new BlockPos(-156, 0, -250), XaeroWorldMapFold.foldBlock(LATTICE, 100, 262));
        assertEquals(new BlockPos(100, 0, -250), XaeroWorldMapFold.foldBlock(TORUS, 100, 262));
    }

    @Test
    void aChunkPastTheZSeamOfALatticeTorusIsWrittenToTheTileTheSkewMovesItTo() {
        assertEquals(new ChunkPos(-4, -3), XaeroWorldMapFold.tileOfChunk(LATTICE, 0, 20));
        assertEquals(new ChunkPos(0, -3), XaeroWorldMapFold.tileOfChunk(TORUS, 0, 20));
        assertEquals(new ChunkPos(0, 5), XaeroWorldMapFold.tileOfChunk(null, 0, 20));
    }

    @Test
    void aRegionPastTheZSeamOfALatticeTorusFoldsOntoTheRegionTheSkewMovesItTo() {
        assertEquals(new ChunkPos(-1, 0), XaeroWorldMapFold.foldRegion(LATTICE, 0, 1));
        assertEquals(new ChunkPos(0, 0), XaeroWorldMapFold.foldRegion(TORUS, 0, 1));
    }

    @Test
    void aComparisonPastTheZSeamOfALatticeTorusFoldsWhole() {
        assertEquals(new ChunkPos(-32, -28), XaeroWorldMapFold.foldComparison(LATTICE, -16, 4));
        assertEquals(new ChunkPos(-16, -28), XaeroWorldMapFold.foldComparison(TORUS, -16, 4));
    }

    @Test
    void aWriteWindowPastTheZSeamOfALatticeTorusVisitsTheRegionBesideIt() {
        assertArrayEquals(new long[] {ChunkPos.pack(-1, -1)}, XaeroWorldMapFold.canonicalRegions(LATTICE, 0, 4, 1, 5),
                "the window lands 256 blocks west, so a per-axis product would have visited region (0, -1)");
        assertArrayEquals(new long[] {ChunkPos.pack(0, -1)}, XaeroWorldMapFold.canonicalRegions(TORUS, 0, 4, 1, 5));
        assertArrayEquals(new long[] {ChunkPos.pack(0, 0)}, XaeroWorldMapFold.canonicalRegions(null, 0, 4, 1, 5));
    }

    @Test
    void aSlotGluesOnlyWhereTheSkewIsAWholeNumberOfSlots() {
        assertTrue(XaeroWorldMapFold.glueableAt(TORUS, 256));
        assertTrue(XaeroWorldMapFold.glueableAt(CYLINDER, 256));
        assertTrue(XaeroWorldMapFold.glueableAt(LATTICE, 256));
        assertFalse(XaeroWorldMapFold.glueableAt(HALF_SKEW_LATTICE, 256),
                "a 128-block skew moves a 256-block slot past the Z seam across two canonical slots");
        assertTrue(XaeroWorldMapFold.glueableAt(HALF_SKEW_LATTICE, 128));
        assertFalse(XaeroWorldMapFold.glueableAt(null, 256));
    }
}
