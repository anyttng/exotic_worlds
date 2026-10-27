package com.exoticworlds.core;

import static com.exoticworlds.core.WorldFoldFixture.EVEN;
import static com.exoticworlds.core.WorldFoldFixture.PER_AXIS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

class SeamDistanceTest {
    private static final long SEED = 0xD157L;
    private static final int SAMPLES = 1500;
    private static final int LAPS = 5;

    private static final List<WorldFold> FOLDS = PER_AXIS;

    private static int sampleChunk(Random random, WrapDomain domain) {
        int reach = 3 * (domain instanceof WrapDomain.Noop ? 1_000 : Math.min(domain.domainLength, 1_000));
        return random.nextInt(2 * reach + 1) - reach;
    }

    private static double sampleBlock(Random random, WrapDomain domain) {
        int reach = 3 * (domain instanceof WrapDomain.Noop ? 16_000 : Math.min(domain.domainLength, 16_000));
        return random.nextInt(2 * reach + 1) - reach + random.nextDouble();
    }

    private static int sampleBlockCoord(Random random, WrapDomain domain) {
        return (int) Math.floor(sampleBlock(random, domain));
    }

    private static double sampleY(Random random) {
        return random.nextInt(384) - 64 + random.nextDouble();
    }

    private static int sampleYBlock(Random random) {
        return random.nextInt(384) - 64;
    }

    private static double coordSqrRef(WorldFold fold, double dx, double dy, double dz) {
        double best = Double.MAX_VALUE;
        for (int xLaps = -LAPS; xLaps <= LAPS; xLaps++) {
            for (int zLaps = -LAPS; zLaps <= LAPS; zLaps++) {
                double shiftedX = dx + xLaps * (double) blockX(fold).domainLength;
                double shiftedZ = dz + zLaps * (double) blockZ(fold).domainLength;
                best = Math.min(best, shiftedX * shiftedX + dy * dy + shiftedZ * shiftedZ);
            }
        }
        return best;
    }

    private static long chunkSqrRef(WorldFold fold, int dx, int dz) {
        long best = Long.MAX_VALUE;
        for (int xLaps = -LAPS; xLaps <= LAPS; xLaps++) {
            for (int zLaps = -LAPS; zLaps <= LAPS; zLaps++) {
                long shiftedX = dx + (long) xLaps * chunkX(fold).domainLength;
                long shiftedZ = dz + (long) zLaps * chunkZ(fold).domainLength;
                best = Math.min(best, shiftedX * shiftedX + shiftedZ * shiftedZ);
            }
        }
        return best;
    }

    private static double flatBoxSqr(AABB box, Vec3 point) {
        double xGap = Math.max(Math.max(box.minX - point.x, point.x - box.maxX), 0.0);
        double yGap = Math.max(Math.max(box.minY - point.y, point.y - box.maxY), 0.0);
        double zGap = Math.max(Math.max(box.minZ - point.z, point.z - box.maxZ), 0.0);
        return xGap * xGap + yGap * yGap + zGap * zGap;
    }

    private static double boxSqrRef(WorldFold fold, AABB box, Vec3 point) {
        double best = Double.MAX_VALUE;
        for (int xLaps = -LAPS; xLaps <= LAPS; xLaps++) {
            for (int zLaps = -LAPS; zLaps <= LAPS; zLaps++) {
                AABB copy = box.move(
                        xLaps * (double) blockX(fold).domainLength,
                        0.0,
                        zLaps * (double) blockZ(fold).domainLength);
                best = Math.min(best, flatBoxSqr(copy, point));
            }
        }
        return best;
    }

    private static String in(WorldFold fold) {
        return "in " + fold;
    }

    private static WrapDomain blockX(WorldFold fold) {
        return fold.blockDomain(Direction.Axis.X);
    }

    private static WrapDomain blockZ(WorldFold fold) {
        return fold.blockDomain(Direction.Axis.Z);
    }

    private static WrapDomain chunkX(WorldFold fold) {
        return fold.chunkDomain(Direction.Axis.X);
    }

    private static WrapDomain chunkZ(WorldFold fold) {
        return fold.chunkDomain(Direction.Axis.Z);
    }

    @Nested
    class PointDistance {
        @Test
        void coordSqrDistIsTheMinimumOverWorldCopies() {
            Random random = new Random(SEED);
            for (WorldFold fold : FOLDS) {
                for (int i = 0; i < SAMPLES; i++) {
                    double xFrom = blockX(fold).wrap(sampleBlock(random, blockX(fold)));
                    double yFrom = sampleY(random);
                    double zFrom = blockZ(fold).wrap(sampleBlock(random, blockZ(fold)));
                    double xTo = blockX(fold).wrap(sampleBlock(random, blockX(fold)));
                    double yTo = sampleY(random);
                    double zTo = blockZ(fold).wrap(sampleBlock(random, blockZ(fold)));

                    double expected = coordSqrRef(fold, xTo - xFrom, yTo - yFrom, zTo - zFrom);
                    double actual = fold.sqrDistance(xFrom, yFrom, zFrom, xTo, yTo, zTo);
                    assertEquals(expected, actual, 1e-3, () -> "Coord.sqrDistToBounds " + in(fold));
                }
            }
        }

        @Test
        void chunkSqrDistMatchesTheLattice() {
            Random random = new Random(SEED);
            for (WorldFold fold : FOLDS) {
                for (int i = 0; i < SAMPLES; i++) {
                    int xFrom = chunkX(fold).wrap(sampleChunk(random, chunkX(fold)));
                    int zFrom = chunkZ(fold).wrap(sampleChunk(random, chunkZ(fold)));
                    int xTo = chunkX(fold).wrap(sampleChunk(random, chunkX(fold)));
                    int zTo = chunkZ(fold).wrap(sampleChunk(random, chunkZ(fold)));

                    long expected = chunkSqrRef(fold, xTo - xFrom, zTo - zFrom);
                    int actual = fold.sqrChunkDistance(new ChunkPos(xFrom, zFrom), new ChunkPos(xTo, zTo));
                    assertEquals(expected, actual, () -> "sqrChunkDistance " + in(fold));
                }
            }
        }

        @Test
        void chessboardDistanceIsTheMinimumOverWorldCopiesHoweverFarOutTheTarget() {
            Random random = new Random(SEED);
            for (WorldFold fold : FOLDS) {
                for (int i = 0; i < SAMPLES; i++) {
                    ChunkPos from = new ChunkPos(
                            chunkX(fold).wrap(sampleChunk(random, chunkX(fold))),
                            chunkZ(fold).wrap(sampleChunk(random, chunkZ(fold))));
                    ChunkPos to = new ChunkPos(
                            sampleChunk(random, chunkX(fold)),
                            sampleChunk(random, chunkZ(fold)));

                    int wrappedX = chunkX(fold).wrap(to.x);
                    int wrappedZ = chunkZ(fold).wrap(to.z);
                    int expected = Integer.MAX_VALUE;
                    for (int xLaps = -1; xLaps <= 1; xLaps++) {
                        for (int zLaps = -1; zLaps <= 1; zLaps++) {
                            int dx = Math.abs(wrappedX + xLaps * chunkX(fold).domainLength - from.x);
                            int dz = Math.abs(wrappedZ + zLaps * chunkZ(fold).domainLength - from.z);
                            expected = Math.min(expected, Math.max(dx, dz));
                        }
                    }

                    int finalExpected = expected;
                    assertEquals(expected, from.getChessboardDistance(fold.nearestCopy(from, to)),
                            () -> "chessboardDistance(" + from + ", " + to + ") should be " + finalExpected + " "
                                    + in(fold));
                }
            }
        }
    }

    private static int refClamp(WrapDomain domain, int coord) {
        return domain instanceof WrapDomain.Noop ? coord
                : Math.min(Math.max(coord, domain.lowerBound), domain.upperBound - 1);
    }

    @Nested
    class Overshoot {
        @Test
        void overshootIsTheChessboardDistanceToTheNearestChunkStillInside() {
            Random random = new Random(SEED);
            for (WorldFold fold : FOLDS) {
                for (int i = 0; i < SAMPLES; i++) {
                    int x = sampleChunk(random, chunkX(fold));
                    int z = sampleChunk(random, chunkZ(fold));
                    int expected = Math.max(
                            Math.abs(x - refClamp(chunkX(fold), x)),
                            Math.abs(z - refClamp(chunkZ(fold), z)));

                    assertEquals(expected, fold.chunkOvershoot(new ChunkPos(x, z)),
                            () -> "overshoot(" + x + ", " + z + ") " + in(fold));
                }
            }
        }

        @Test
        void insideTheWorldOvershootIsZero() {
            for (WorldFold fold : FOLDS) {
                assertEquals(0, fold.chunkOvershoot(new ChunkPos(0, 0)), () -> in(fold));
                assertEquals(0, fold.chunkOvershoot(new ChunkPos(
                        chunkX(fold).lowerBound,
                        chunkZ(fold).upperBound - 1)), () -> in(fold));
            }
        }
    }

    @Nested
    class NearestCopy {
        @Test
        void landsOnTheSamePositionNoFurtherThanAnyOtherCopyAndWithinHalfAWorld() {
            Random random = new Random(SEED);
            for (WorldFold fold : FOLDS) {
                for (int i = 0; i < SAMPLES; i++) {
                    Vec3 ref = new Vec3(
                            sampleBlock(random, blockX(fold)),
                            sampleY(random),
                            sampleBlock(random, blockZ(fold)));
                    Vec3 target = new Vec3(
                            sampleBlock(random, blockX(fold)),
                            sampleY(random),
                            sampleBlock(random, blockZ(fold)));

                    Vec3 nearest = fold.nearestCopy(ref, target);

                    assertEquals(target.y, nearest.y, 0.0, () -> "Y is seamless " + in(fold));
                    assertEquals(blockX(fold).wrap(target.x), blockX(fold).wrap(nearest.x), 1e-6,
                            () -> "nearestCopy moved the X position " + in(fold));
                    assertEquals(blockZ(fold).wrap(target.z), blockZ(fold).wrap(nearest.z), 1e-6,
                            () -> "nearestCopy moved the Z position " + in(fold));

                    double expected = coordSqrRef(fold,
                            blockX(fold).wrap(target.x) - ref.x, 0.0,
                            blockZ(fold).wrap(target.z) - ref.z);
                    double actual = (nearest.x - ref.x) * (nearest.x - ref.x)
                            + (nearest.z - ref.z) * (nearest.z - ref.z);
                    assertEquals(expected, actual, 1e-3,
                            () -> "nearestCopy(" + ref + ", " + target + ") is not the nearest copy " + in(fold));

                    if (!(blockX(fold) instanceof WrapDomain.Noop)) {
                        assertTrue(Math.abs(nearest.x - ref.x) <= blockX(fold).domainLength / 2.0 + 1e-9,
                                () -> "nearestCopy is over half a world away on X " + in(fold));
                    }
                    if (!(blockZ(fold) instanceof WrapDomain.Noop)) {
                        assertTrue(Math.abs(nearest.z - ref.z) <= blockZ(fold).domainLength / 2.0 + 1e-9,
                                () -> "nearestCopy is over half a world away on Z " + in(fold));
                    }
                }
            }
        }

        @Test
        void blocksLandOnTheSameBlockNoFurtherThanAnyOtherCopyAndWithinHalfAWorld() {
            Random random = new Random(SEED);
            for (WorldFold fold : FOLDS) {
                for (int i = 0; i < SAMPLES; i++) {
                    BlockPos ref = new BlockPos(
                            sampleBlockCoord(random, blockX(fold)),
                            sampleYBlock(random),
                            sampleBlockCoord(random, blockZ(fold)));
                    BlockPos target = new BlockPos(
                            sampleBlockCoord(random, blockX(fold)),
                            sampleYBlock(random),
                            sampleBlockCoord(random, blockZ(fold)));

                    BlockPos nearest = fold.nearestCopy(ref, target);

                    assertEquals(target.getY(), nearest.getY(), () -> "Y is seamless " + in(fold));
                    assertEquals(blockX(fold).wrap(target.getX()), blockX(fold).wrap(nearest.getX()),
                            () -> "nearestCopy moved the X block " + in(fold));
                    assertEquals(blockZ(fold).wrap(target.getZ()), blockZ(fold).wrap(nearest.getZ()),
                            () -> "nearestCopy moved the Z block " + in(fold));

                    double expected = coordSqrRef(fold,
                            blockX(fold).wrap(target.getX()) - (double) ref.getX(), 0.0,
                            blockZ(fold).wrap(target.getZ()) - (double) ref.getZ());
                    double dx = nearest.getX() - (double) ref.getX();
                    double dz = nearest.getZ() - (double) ref.getZ();
                    assertEquals(expected, dx * dx + dz * dz, 1e-3,
                            () -> "nearestCopy(" + ref + ", " + target + ") is not the nearest copy " + in(fold));

                    if (!(blockX(fold) instanceof WrapDomain.Noop)) {
                        assertTrue(Math.abs(dx) <= blockX(fold).domainLength / 2.0,
                                () -> "nearestCopy is over half a world away on X " + in(fold));
                    }
                    if (!(blockZ(fold) instanceof WrapDomain.Noop)) {
                        assertTrue(Math.abs(dz) <= blockZ(fold).domainLength / 2.0,
                                () -> "nearestCopy is over half a world away on Z " + in(fold));
                    }
                }
            }
        }
    }

    @Nested
    class BoxDistance {
        @Test
        void distanceToSqrWrappedCoordMatchesTheNearestBoxCopy() {
            Random random = new Random(SEED);
            for (WorldFold fold : FOLDS) {
                for (int i = 0; i < SAMPLES; i++) {
                    double minX = blockX(fold).wrap(sampleBlock(random, blockX(fold)));
                    double minY = sampleY(random);
                    double minZ = blockZ(fold).wrap(sampleBlock(random, blockZ(fold)));
                    AABB box = new AABB(minX, minY, minZ,
                            minX + random.nextDouble() * 8, minY + random.nextDouble() * 8, minZ + random.nextDouble() * 8);
                    Vec3 point = new Vec3(
                            blockX(fold).wrap(sampleBlock(random, blockX(fold))),
                            sampleY(random),
                            blockZ(fold).wrap(sampleBlock(random, blockZ(fold))));

                    assertEquals(boxSqrRef(fold, box, point),
                            fold.sqrDistanceToBox(box, point), 1e-3,
                            () -> "distanceToSqrWrappedCoord(" + box + ", " + point + ") " + in(fold));
                }
            }
        }

        @Test
        void aBlockJustAcrossTheSeamIsAStepAwayNotAWorldAway() {
            AABB box = new AABB(-512, 0, 0, -502, 10, 10);
            Vec3 point = new Vec3(500, 5, 5);
            assertEquals(144, EVEN.sqrDistanceToBox(box, point), 1e-9);
        }

        @Test
        void aPointInsideTheBoxIsAtDistanceZero() {
            AABB box = new AABB(-5, 0, -5, 5, 10, 5);
            for (WorldFold fold : FOLDS) {
                assertEquals(0.0, fold.sqrDistanceToBox(box, new Vec3(0, 5, 0)), 0.0,
                        () -> in(fold));
            }
        }
    }

    @Nested
    class NearestCopyOrientedBox {
        @Test
        void movesByWholeWorldWidthsToTheCopyNearestTheReference() {
            Random random = new Random(SEED);
            for (WorldFold fold : FOLDS) {
                for (int i = 0; i < SAMPLES; i++) {
                    double minX = sampleBlock(random, blockX(fold));
                    double minY = sampleY(random);
                    double minZ = sampleBlock(random, blockZ(fold));
                    AABB box = new AABB(minX, minY, minZ,
                            minX + random.nextDouble() * 8, minY + 1, minZ + random.nextDouble() * 8);
                    Vec3 ref = new Vec3(
                            blockX(fold).wrap(sampleBlock(random, blockX(fold))),
                            sampleY(random),
                            blockZ(fold).wrap(sampleBlock(random, blockZ(fold))));

                    AABB folded = fold.nearestCopyOriented(ref, box).value();

                    assertEquals(box.maxX - box.minX, folded.maxX - folded.minX, 1e-9, () -> "X size " + in(fold));
                    assertEquals(box.maxZ - box.minZ, folded.maxZ - folded.minZ, 1e-9, () -> "Z size " + in(fold));
                    assertEquals(box.minY, folded.minY, 0.0, () -> "Y floor " + in(fold));
                    assertEquals(box.maxY, folded.maxY, 0.0, () -> "Y ceiling " + in(fold));

                    checkShiftOnLattice(blockX(fold), box.minX, folded.minX, "X", fold);
                    checkShiftOnLattice(blockZ(fold), box.minZ, folded.minZ, "Z", fold);

                    double centerX = (box.minX + box.maxX) / 2.0;
                    double centerZ = (box.minZ + box.maxZ) / 2.0;
                    double foldedCenterX = (folded.minX + folded.maxX) / 2.0;
                    double foldedCenterZ = (folded.minZ + folded.maxZ) / 2.0;
                    double expected = coordSqrRef(fold,
                            blockX(fold).wrap(centerX) - ref.x, 0.0,
                            blockZ(fold).wrap(centerZ) - ref.z);
                    double actual = (foldedCenterX - ref.x) * (foldedCenterX - ref.x)
                            + (foldedCenterZ - ref.z) * (foldedCenterZ - ref.z);
                    assertEquals(expected, actual, 1e-3,
                            () -> "nearestCopyOrientedToward did not pick the nearest copy " + in(fold));
                }
            }
        }

        private void checkShiftOnLattice(WrapDomain domain, double boxMin, double foldedMin,
                String axis, WorldFold fold) {
            if (domain instanceof WrapDomain.Noop) {
                assertEquals(boxMin, foldedMin, 0.0,
                        () -> axis + " shift moved an unbounded axis " + in(fold));
                return;
            }

            double shiftLaps = (foldedMin - boxMin) / domain.domainLength;
            assertEquals((double) Math.round(shiftLaps), shiftLaps, 1e-9,
                    () -> axis + " shift left its lattice " + in(fold));
        }

        @Test
        void aBoxAlreadyNearestComesBackAsTheSameInstance() {
            AABB box = new AABB(3, 0, 3, 6, 2, 6);
            Vec3 ref = new Vec3(0, 1, 0);
            for (WorldFold fold : FOLDS) {
                assertSame(box, fold.nearestCopyOriented(ref, box).value(), () -> in(fold));
            }
        }
    }

    @Nested
    class DisabledEverywhere {
        private final WorldFold disabled = WorldFolds.NOOP;

        @Test
        void everyDistanceIsThePlainOne() {
            Random random = new Random(SEED);
            for (int i = 0; i < SAMPLES; i++) {
                double dx = random.nextInt(20_001) - 10_000 + random.nextDouble();
                double dy = sampleY(random);
                double dz = random.nextInt(20_001) - 10_000 + random.nextDouble();
                assertEquals(dx * dx + dy * dy + dz * dz,
                        disabled.sqrDistance(0, 0, 0, dx, dy, dz), 1e-3);

                ChunkPos from = new ChunkPos(random.nextInt(2_001) - 1_000, random.nextInt(2_001) - 1_000);
                ChunkPos to = new ChunkPos(random.nextInt(2_001) - 1_000, random.nextInt(2_001) - 1_000);
                long flatDx = (long) to.x - from.x;
                long flatDz = (long) to.z - from.z;
                assertEquals(flatDx * flatDx + flatDz * flatDz, disabled.sqrChunkDistance(from, to));
                assertEquals(from.getChessboardDistance(to), from.getChessboardDistance(disabled.nearestCopy(from, to)));

                Vec3 ref = new Vec3(dx, dy, dz);
                Vec3 target = new Vec3(dz, dy, dx);
                assertEquals(target, disabled.nearestCopy(ref, target));

                AABB box = new AABB(dx, dy, dz, dx + 3, dy + 3, dz + 3);
                assertEquals(flatBoxSqr(box, ref), disabled.sqrDistanceToBox(box, ref), 1e-9);
                assertSame(box, disabled.nearestCopyOriented(ref, box).value());
            }
        }
    }
}
