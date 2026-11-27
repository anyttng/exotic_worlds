package com.exoticworlds.compat.lithostitched;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.fold.TranslationLattice;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.core.world.WorldLoopBounds;

import net.minecraft.core.Direction;

class CellLapTest {
    private static final int TINY_CHUNKS = 32;

    private static final int WIDE_CHUNKS = 48;

    private static final int SKEW_CHUNKS = 4;

    private static final int BLOCKS_PER_CHUNK = 16;

    private static final int[][] CELLS = {{0, 0}, {3, -2}, {-7, 5}, {11, 13}, {-1, -1}};

    private static WorldFold torus(int xChunks, int zChunks) {
        return WorldFolds.of(ShapeFixtures.torus(WorldLoopBounds.ofWidths(xChunks, zChunks)));
    }

    @Test
    void aWidthThatDoesNotDivideTheLapShrinksToTheWidestDivisorBelowIt() {
        assertEquals(64, CellLap.of(torus(TINY_CHUNKS, TINY_CHUNKS), 96).widthBlocks());
        assertEquals(32, CellLap.of(torus(TINY_CHUNKS, TINY_CHUNKS), 48).widthBlocks());
        assertEquals(128, CellLap.of(torus(TINY_CHUNKS, TINY_CHUNKS), 128).widthBlocks());
    }

    @Test
    void aCellWiderThanTheWorldBecomesOneLap() {
        assertEquals(TINY_CHUNKS * BLOCKS_PER_CHUNK, CellLap.of(torus(TINY_CHUNKS, TINY_CHUNKS), 700).widthBlocks());
    }

    @Test
    void unevenLapsShareOneWidth() {
        assertEquals(256, CellLap.of(torus(TINY_CHUNKS, WIDE_CHUNKS), 300).widthBlocks());
    }

    @Test
    void aCylinderFitsItsLoopingAxisAlone() {
        WorldFold cylinder = WorldFolds.of(ShapeFixtures.cylinder(WorldLoopBounds.ofWidth(Direction.Axis.X,
                TINY_CHUNKS)));
        assertEquals(64, CellLap.of(cylinder, 96).widthBlocks());
    }

    @Test
    void aCellOneLapAwayIsTheSameCell() {
        CellLap lap = CellLap.of(torus(TINY_CHUNKS, WIDE_CHUNKS), 96);
        int xCells = TINY_CHUNKS * BLOCKS_PER_CHUNK / lap.widthBlocks();
        int zCells = WIDE_CHUNKS * BLOCKS_PER_CHUNK / lap.widthBlocks();
        for (int[] cell : CELLS) {
            assertEquals(lap.foldX(cell[0], cell[1]), lap.foldX(cell[0] + xCells, cell[1]));
            assertEquals(lap.foldX(cell[0], cell[1]), lap.foldX(cell[0], cell[1] + zCells));
            assertEquals(lap.foldZ(cell[1]), lap.foldZ(cell[1] - zCells));
        }
    }

    @Test
    void onASkewedTorusTheZLapCarriesTheSkewInWholeCells() {
        WorldFold fold = WorldFolds.of(FlatShape.latticeTorus(ShapeFixtures.loopedBounds(-16, 16, -16, 16),
                SKEW_CHUNKS));
        TranslationLattice lattice = fold.getBlockLattice();
        CellLap lap = CellLap.of(fold, 96);
        assertEquals(64, lap.widthBlocks());
        int xCells = lattice.getX().domainLength / lap.widthBlocks();
        int zCells = lattice.getZ().domainLength / lap.widthBlocks();
        int skewCells = lattice.getSkew() / lap.widthBlocks();
        for (int[] cell : CELLS) {
            assertEquals(lap.foldX(cell[0], cell[1]), lap.foldX(cell[0] + skewCells, cell[1] + zCells));
            assertEquals(lap.foldZ(cell[1]), lap.foldZ(cell[1] + zCells));
            assertEquals(lap.foldX(cell[0], cell[1]), lap.foldX(cell[0] + xCells, cell[1]));
        }
    }
}
