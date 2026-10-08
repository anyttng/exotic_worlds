package com.exoticworlds.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

class TranslationLatticeTest {
    private static final WorldLoopBounds BOUNDS = new WorldLoopBounds(-16, 16, -12, 12);
    private static final int SKEW_CHUNKS = 5;
    private static final DeckGroupFold SKEWED = new DeckGroupFold(FlatShape.latticeTorus(BOUNDS, SKEW_CHUNKS));
    private static final TranslationLattice LATTICE = SKEWED.blockLattice();

    private static final int WIDTH_X = 512;
    private static final int WIDTH_Z = 384;
    private static final int SKEW_BLOCKS = SKEW_CHUNKS * CoordinateConstants.CHUNK_WIDTH;
    private static final int SWEEP = 1500;
    private static final int STEP = 37;

    @Test
    void theJointFoldIsTheDeckGroupsFold() {
        for (int x = -SWEEP; x <= SWEEP; x += STEP) {
            for (int z = -SWEEP; z <= SWEEP; z += STEP) {
                BlockPos folded = SKEWED.fold(new BlockPos(x, 0, z));
                assertEquals(folded.getX(), LATTICE.foldX(x, z), "x at " + x + ", " + z);
                assertEquals(folded.getZ(), LATTICE.foldZ(z), "z at " + x + ", " + z);
            }
        }
    }

    @Test
    void everyCopyFoldsOntoTheSamePoint() {
        for (int x = -SWEEP; x <= SWEEP; x += STEP) {
            for (int z = -SWEEP; z <= SWEEP; z += STEP) {
                int foldedX = LATTICE.foldX(x, z);
                int foldedZ = LATTICE.foldZ(z);
                assertEquals(foldedX, LATTICE.foldX(x + WIDTH_X, z));
                assertEquals(foldedX, LATTICE.foldX(x + SKEW_BLOCKS, z + WIDTH_Z));
                assertEquals(foldedZ, LATTICE.foldZ(z + WIDTH_Z));
                double fractionX = x + 0.375;
                double fractionZ = z + 0.625;
                assertEquals(LATTICE.foldX(fractionX, fractionZ),
                        LATTICE.foldX(fractionX + SKEW_BLOCKS, fractionZ + WIDTH_Z));
                assertEquals(LATTICE.foldZ(fractionZ), LATTICE.foldZ(fractionZ + WIDTH_Z));
            }
        }
    }

    @Test
    void theNearestDeltaIsAsShortAsTheDeckGroupsNearestCopy() {
        BlockPos origin = BlockPos.ZERO;
        for (int x = -SWEEP; x <= SWEEP; x += STEP) {
            for (int z = -SWEEP; z <= SWEEP; z += STEP) {
                int laps = LATTICE.nearestZLaps(x, z);
                long deltaX = LATTICE.nearestDeltaX(x, laps);
                long deltaZ = LATTICE.nearestDeltaZ(z, laps);
                BlockPos nearest = SKEWED.nearestCopy(origin, new BlockPos(x, 0, z));
                long expected = (long) nearest.getX() * nearest.getX() + (long) nearest.getZ() * nearest.getZ();
                assertEquals(expected, deltaX * deltaX + deltaZ * deltaZ, "at " + x + ", " + z);
            }
        }
    }

    @Test
    void anUnskewedNearestDeltaIsTheAxisFold() {
        TranslationLattice plain = WorldFolds.of(FlatShape.torus(BOUNDS)).blockLattice();
        WrapDomain xDomain = plain.x();
        WrapDomain zDomain = plain.z();
        for (int x = -WIDTH_X + 1; x < WIDTH_X; x += STEP) {
            for (int z = -WIDTH_Z + 1; z < WIDTH_Z; z += STEP) {
                int laps = plain.nearestZLaps(x, z);
                assertEquals(xDomain.foldDelta(x), plain.nearestDeltaX(x, laps));
                assertEquals(zDomain.foldDelta(z), plain.nearestDeltaZ(z, laps));
            }
        }
    }

    @Test
    void aMirrorIsNoTranslationLattice() {
        DeckGroupFold mirrored = new DeckGroupFold(FlatShape.mirrored(BOUNDS, Direction.Axis.Z, 0));
        assertThrows(IllegalStateException.class, mirrored::blockLattice);
        assertThrows(IllegalStateException.class, mirrored::chunkLattice);
    }
}
