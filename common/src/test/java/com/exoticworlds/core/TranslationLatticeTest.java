package com.exoticworlds.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Random;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

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

    static final int[][] LONG_SHAPES = {
            {1024, 32, 100}, {1024, 16, 300}, {1024, 16, -511}, {2048, 16, 777}, {64, 48, 5}, {1024, 16, 0}};
    private static final long LONG_SEED = 20261009L;
    private static final int LONG_SAMPLES = 4000;
    private static final double PRECISION = 1.0E-6;

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
    void onALongSkewedLatticeTheNearestDeltaIsTheShortestCopy() {
        for (int[] shape : LONG_SHAPES) {
            TranslationLattice lattice = longFold(shape).blockLattice();
            Random random = new Random(LONG_SEED);
            for (int sample = 0; sample < LONG_SAMPLES; sample++) {
                int x = random.nextInt(-lattice.x().domainLength, lattice.x().domainLength);
                int z = random.nextInt(-3 * lattice.z().domainLength, 3 * lattice.z().domainLength);
                int laps = lattice.nearestZLaps(x, z);
                double deltaX = lattice.nearestDeltaX(x, laps);
                double deltaZ = lattice.nearestDeltaZ(z, laps);
                assertEquals(shortestSquared(lattice, x, z), deltaX * deltaX + deltaZ * deltaZ,
                        "at " + x + ", " + z + " on " + Arrays.toString(shape));
            }
        }
    }

    @Test
    void onALongSkewedLatticeTheFoldsNearestCopyIsTheShortestCopy() {
        for (int[] shape : LONG_SHAPES) {
            DeckGroupFold fold = longFold(shape);
            TranslationLattice lattice = fold.blockLattice();
            Random random = new Random(LONG_SEED);
            for (int sample = 0; sample < LONG_SAMPLES; sample++) {
                int x = random.nextInt(-lattice.x().domainLength, lattice.x().domainLength);
                int z = random.nextInt(-3 * lattice.z().domainLength, 3 * lattice.z().domainLength);
                BlockPos block = fold.nearestCopy(BlockPos.ZERO, new BlockPos(x, 0, z));
                assertEquals(shortestSquared(lattice, x, z),
                        (double) block.getX() * block.getX() + (double) block.getZ() * block.getZ(),
                        "block at " + x + ", " + z + " on " + Arrays.toString(shape));
                Vec3 point = fold.nearestCopy(Vec3.ZERO, new Vec3(x + 0.25, 0.0, z + 0.75));
                assertEquals(shortestSquared(lattice, x + 0.25, z + 0.75),
                        point.x * point.x + point.z * point.z, PRECISION,
                        "point at " + x + ", " + z + " on " + Arrays.toString(shape));
            }
        }
    }

    static DeckGroupFold longFold(int[] shape) {
        int halfX = shape[0] / 2;
        int halfZ = shape[1] / 2;
        return new DeckGroupFold(FlatShape.latticeTorus(new WorldLoopBounds(-halfX, halfX, -halfZ, halfZ), shape[2]));
    }

    static double shortestSquared(TranslationLattice lattice, double deltaX, double deltaZ) {
        int widthX = lattice.x().domainLength;
        int widthZ = lattice.z().domainLength;
        long centre = Math.round(deltaZ / widthZ);
        long reach = widthX / widthZ + 2L;
        double shortest = Double.MAX_VALUE;
        for (long laps = centre - reach; laps <= centre + reach; laps++) {
            double z = deltaZ - (double) laps * widthZ;
            double x = deltaX - (double) laps * lattice.skew();
            double lapped = x - Math.floor(x / widthX) * widthX;
            double nearestX = Math.min(lapped, widthX - lapped);
            shortest = Math.min(shortest, nearestX * nearestX + z * z);
        }

        return shortest;
    }

    @Test
    void aMirrorIsNoTranslationLattice() {
        DeckGroupFold mirrored = new DeckGroupFold(FlatShape.mirrored(BOUNDS, Direction.Axis.Z, 0));
        assertThrows(IllegalStateException.class, mirrored::blockLattice);
        assertThrows(IllegalStateException.class, mirrored::chunkLattice);
    }
}
