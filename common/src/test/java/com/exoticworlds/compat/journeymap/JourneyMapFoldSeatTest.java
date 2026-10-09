package com.exoticworlds.compat.journeymap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.MapCopies;
import com.exoticworlds.compat.MapShapes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

class JourneyMapFoldSeatTest {
    private static final ToroidalShape LATTICE = MapShapes.latticeTorus(-16, 16, 16);
    private static final ToroidalShape TORUS = MapShapes.torus(-16, 16);
    private static final ToroidalShape CYLINDER = MapShapes.cylinder(-16, 16);
    private static final int WINDOW_PIXELS = 512;
    private static final int ZOOM = 2048;

    @Test
    void aChunkPastTheZSeamOfALatticeTorusLandsInTheRegionTheSkewMovesItTo() {
        assertEquals(new ChunkPos(-16, -12), JourneyMapFold.foldRegionChunk(LATTICE, new ChunkPos(0, 20)),
                "crossing the Z seam moves X by 16 chunks, so a per-axis fold would have kept it at 0");
        assertEquals(new ChunkPos(0, -12), JourneyMapFold.foldRegionChunk(TORUS, new ChunkPos(0, 20)));
        assertEquals(new ChunkPos(0, 20), JourneyMapFold.foldRegionChunk(CYLINDER, new ChunkPos(0, 20)));
    }

    @Test
    void aMarkerPastTheZSeamOfALatticeTorusSitsOnTheCopyNearestTheCentre() {
        assertEquals(new Vec3(-156, 0, 262),
                JourneyMapFold.seatPixel(LATTICE, 0, 200, 100, -250, MapCopies.REPEATED),
                "the copy across the Z seam is moved 256 blocks in X by the skew");
        assertEquals(new Vec3(100, 0, 262), JourneyMapFold.seatPixel(TORUS, 0, 200, 100, -250, MapCopies.REPEATED));
        assertEquals(new Vec3(100, 0, -250),
                JourneyMapFold.seatPixel(CYLINDER, 0, 200, 100, -250, MapCopies.REPEATED));
    }

    @Test
    void aSingleMapFoldsAMarkerOfALatticeTorusWhole() {
        assertEquals(new Vec3(100, 0, -250), JourneyMapFold.seatPixel(LATTICE, 0, 0, -156, 262, MapCopies.SINGLE));
    }

    @Test
    void theLocationReadoutOfALatticeTorusNamesTheCanonicalBlock() {
        assertEquals(new BlockPos(-156, 64, -250), JourneyMapFold.foldUiBlock(LATTICE, new BlockPos(100, 64, 262)));
        assertEquals(new BlockPos(100, 64, -250), JourneyMapFold.foldUiBlock(TORUS, new BlockPos(100, 64, 262)));
    }

    @Test
    void aSingleMapCentreOfALatticeTorusFoldsWholeBeforeItIsHeldInside() {
        Vec3 center = JourneyMapFold.foldCenter(LATTICE, 0, 400);
        assertEquals(new Vec3(-256, 0, -112), center);
        assertEquals(-192.0,
                JourneyMapFold.clampSingleCenter(LATTICE, Direction.Axis.X, center.x, ZOOM, WINDOW_PIXELS),
                "a window 64 blocks either side stays inside the canonical rectangle's west edge");
        assertEquals(-112.0,
                JourneyMapFold.clampSingleCenter(LATTICE, Direction.Axis.Z, center.z, ZOOM, WINDOW_PIXELS));
    }

    @Test
    void anUnshapedWorldPassesEveryPositionThrough() {
        ChunkPos chunk = new ChunkPos(0, 20);
        BlockPos block = new BlockPos(100, 64, 262);
        assertSame(chunk, JourneyMapFold.foldRegionChunk(null, chunk));
        assertSame(block, JourneyMapFold.foldUiBlock(null, block));
        assertEquals(new Vec3(100, 0, -250), JourneyMapFold.seatPixel(null, 0, 200, 100, -250, MapCopies.REPEATED));
    }
}
