package com.exoticworlds.compat.lithostitched;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;

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
        assertEquals(64, CellLap.of(torus(TINY_CHUNKS, TINY_CHUNKS), 96).width());
        assertEquals(32, CellLap.of(torus(TINY_CHUNKS, TINY_CHUNKS), 48).width());
        assertEquals(128, CellLap.of(torus(TINY_CHUNKS, TINY_CHUNKS), 128).width());
    }

    @Test
    void aCellWiderThanTheWorldBecomesOneLap() {
        assertEquals(TINY_CHUNKS * BLOCKS_PER_CHUNK, CellLap.of(torus(TINY_CHUNKS, TINY_CHUNKS), 700).width());
    }

    @Test
    void unevenLapsShareOneWidth() {
        assertEquals(256, CellLap.of(torus(TINY_CHUNKS, WIDE_CHUNKS), 300).width());
    }

    @Test
    void aCylinderFitsItsLoopingAxisAlone() {
        WorldFold cylinder = WorldFolds.of(ShapeFixtures.cylinder(WorldLoopBounds.ofWidth(Direction.Axis.X,
                TINY_CHUNKS)));
        assertEquals(64, CellLap.of(cylinder, 96).width());
    }

    @Test
    void aCellOneLapAwayIsTheSameCell() {
        CellLap lap = CellLap.of(torus(TINY_CHUNKS, WIDE_CHUNKS), 96);
        int xCells = TINY_CHUNKS * BLOCKS_PER_CHUNK / lap.width();
        int zCells = WIDE_CHUNKS * BLOCKS_PER_CHUNK / lap.width();
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
        TranslationLattice lattice = fold.blockLattice();
        CellLap lap = CellLap.of(fold, 96);
        assertEquals(64, lap.width());
        int xCells = lattice.x().domainLength / lap.width();
        int zCells = lattice.z().domainLength / lap.width();
        int skewCells = lattice.skew() / lap.width();
        for (int[] cell : CELLS) {
            assertEquals(lap.foldX(cell[0], cell[1]), lap.foldX(cell[0] + skewCells, cell[1] + zCells));
            assertEquals(lap.foldZ(cell[1]), lap.foldZ(cell[1] + zCells));
            assertEquals(lap.foldX(cell[0], cell[1]), lap.foldX(cell[0] + xCells, cell[1]));
        }
    }
}
