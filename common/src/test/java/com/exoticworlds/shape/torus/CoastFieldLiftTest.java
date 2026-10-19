package com.exoticworlds.shape.torus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WrapDomain;

class CoastFieldLiftTest {
    private static final int X_GRID = 8;
    private static final int Z_GRID = 6;
    private static final int SKEW_CELLS = 3;
    private static final TranslationLattice LATTICE =
            new TranslationLattice(new WrapDomain(0, X_GRID), new WrapDomain(0, Z_GRID), SKEW_CELLS);

    private static boolean[] landAt(int[]... cells) {
        boolean[] land = new boolean[X_GRID * Z_GRID];
        for (int[] cell : cells) {
            land[cell[0] * Z_GRID + cell[1]] = true;
        }

        return land;
    }

    private static int[] pastTheTopRow(int x) {
        return new int[] {LATTICE.foldX(x, Z_GRID), LATTICE.foldZ(Z_GRID)};
    }

    @Test
    void landJoinedOnlyAcrossTheSkewedZSeamIsOnePatch() {
        for (int x = 0; x < X_GRID; x++) {
            int[] top = {x, Z_GRID - 1};
            assertEquals(2, CoastFieldLift.largestComponent(landAt(top, pastTheTopRow(x)), X_GRID, Z_GRID, SKEW_CELLS),
                    "top row cell x=" + x);
        }
    }

    @Test
    void theUnskewedCellAcrossTheZSeamIsAnotherPatch() {
        int x = 1;
        int[] top = {x, Z_GRID - 1};
        int[] unskewed = {x, 0};
        assertNotEquals(pastTheTopRow(x)[0], unskewed[0]);
        assertEquals(1, CoastFieldLift.largestComponent(landAt(top, unskewed), X_GRID, Z_GRID, SKEW_CELLS));
    }
}
