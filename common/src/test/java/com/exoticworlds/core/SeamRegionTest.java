package com.exoticworlds.core;

import static com.exoticworlds.core.WorldFoldFixture.EVEN;
import static com.exoticworlds.core.WorldFoldFixture.PER_AXIS;
import static com.exoticworlds.core.WorldFoldFixture.X_ONLY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

class SeamRegionTest {
    private static final long SEED = 0xB0C5L;
    private static final int SAMPLES = 800;
    private static final int LAPS = 16;

    private static final List<WorldFold> FOLDS = PER_AXIS;

    private static int reachCap(WrapDomain domain, int cap) {
        return domain instanceof WrapDomain.Noop ? cap : Math.min(domain.domainLength, cap);
    }

    private static int sampleBlockInt(Random random, WrapDomain domain) {
        int reach = 3 * reachCap(domain, 16_000);
        return random.nextInt(2 * reach + 1) - reach;
    }

    private static double sampleBlock(Random random, WrapDomain domain) {
        return sampleBlockInt(random, domain) + random.nextDouble();
    }

    private static int sampleChunk(Random random, WrapDomain domain) {
        int reach = 3 * reachCap(domain, 1_000);
        return random.nextInt(2 * reach + 1) - reach;
    }

    private static double sampleY(Random random) {
        return random.nextInt(384) - 64 + random.nextDouble();
    }

    private static boolean containsOnLattice(WrapDomain domain, double min, double max, double coord) {
        for (int laps = -LAPS; laps <= LAPS; laps++) {
            double shifted = coord + laps * (double) domain.domainLength;
            if (shifted >= min - 1e-9 && shifted <= max + 1e-9) return true;
        }
        return false;
    }

    private static boolean containsOnLattice(WrapDomain domain, long min, long max, long coord) {
        for (int laps = -LAPS; laps <= LAPS; laps++) {
            long shifted = coord + (long) laps * domain.domainLength;
            if (shifted >= min && shifted <= max) return true;
        }
        return false;
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

    private static List<AABB> splitAcrossBounds(WorldFold fold, AABB box) {
        return fold.split(box).stream().map(WorldFold.Folded::value).toList();
    }

    private static List<BoundingBox> splitAcrossBounds(WorldFold fold, BoundingBox region) {
        return fold.split(region).stream().map(WorldFold.Folded::value).toList();
    }

    @Nested
    class SplitAcrossBounds {
        private AABB sampleBox(Random random, WorldFold fold) {
            double minX = sampleBlock(random, blockX(fold));
            double minY = sampleY(random);
            double minZ = sampleBlock(random, blockZ(fold));
            double sizeX = random.nextDouble() * 2.2 * reachCap(blockX(fold), 16_000);
            double sizeZ = random.nextDouble() * 2.2 * reachCap(blockZ(fold), 16_000);
            return new AABB(minX, minY, minZ, minX + sizeX, minY + random.nextDouble() * 16, minZ + sizeZ);
        }

        private double coveredSize(WrapDomain domain, double size) {
            return domain instanceof WrapDomain.Noop ? size : Math.min(size, domain.domainLength);
        }

        @Test
        void partsCoverTheBoxVolumeCappedAtOneWorldPerAxis() {
            Random random = new Random(SEED);
            for (WorldFold fold : FOLDS) {
                for (int i = 0; i < SAMPLES; i++) {
                    AABB box = sampleBox(random, fold);
                    List<AABB> parts = splitAcrossBounds(fold, box);

                    double expected = coveredSize(blockX(fold), box.maxX - box.minX)
                            * (box.maxY - box.minY)
                            * coveredSize(blockZ(fold), box.maxZ - box.minZ);
                    double total = 0;
                    for (AABB part : parts) {
                        total += (part.maxX - part.minX) * (part.maxY - part.minY) * (part.maxZ - part.minZ);
                    }
                    assertEquals(expected, total, 0.5,
                            () -> "splitAcrossBounds(" + box + ") volumes " + in(fold));
                }
            }
        }

        @Test
        void everyPartLiesInsideTheBoundsAndKeepsTheBoxHeight() {
            Random random = new Random(SEED);
            for (WorldFold fold : FOLDS) {
                for (int i = 0; i < SAMPLES; i++) {
                    AABB box = sampleBox(random, fold);
                    for (AABB part : splitAcrossBounds(fold, box)) {
                        if (!(blockX(fold) instanceof WrapDomain.Noop)) {
                            assertTrue(part.minX >= blockX(fold).lowerBound - 1e-9
                                            && part.maxX <= blockX(fold).upperBound + 1e-9,
                                    () -> "part " + part + " of " + box + " leaves the X bounds " + in(fold));
                        }
                        if (!(blockZ(fold) instanceof WrapDomain.Noop)) {
                            assertTrue(part.minZ >= blockZ(fold).lowerBound - 1e-9
                                            && part.maxZ <= blockZ(fold).upperBound + 1e-9,
                                    () -> "part " + part + " of " + box + " leaves the Z bounds " + in(fold));
                        }
                        assertEquals(box.minY, part.minY, 0.0, () -> in(fold));
                        assertEquals(box.maxY, part.maxY, 0.0, () -> in(fold));
                    }
                }
            }
        }

        @Test
        void containmentAgreesWithTheLatticeOfBoxCopies() {
            Random random = new Random(SEED);
            for (WorldFold fold : FOLDS) {
                for (int i = 0; i < SAMPLES; i++) {
                    AABB box = sampleBox(random, fold);
                    List<AABB> parts = splitAcrossBounds(fold, box);
                    for (int p = 0; p < 4; p++) {
                        double x = sampleBlock(random, blockX(fold));
                        double z = sampleBlock(random, blockZ(fold));
                        boolean expected = containsOnLattice(blockX(fold), box.minX, box.maxX, x)
                                && containsOnLattice(blockZ(fold), box.minZ, box.maxZ, z);

                        double wrappedX = blockX(fold).wrap(x);
                        double wrappedZ = blockZ(fold).wrap(z);
                        boolean actual = parts.stream().anyMatch(part -> wrappedX >= part.minX - 1e-9
                                && wrappedX <= part.maxX + 1e-9
                                && wrappedZ >= part.minZ - 1e-9
                                && wrappedZ <= part.maxZ + 1e-9);

                        boolean finalExpected = expected;
                        assertEquals(expected, actual,
                                () -> "(" + x + ", " + z + ") in splitAcrossBounds(" + box + ") should be "
                                        + finalExpected + " " + in(fold));
                    }
                }
            }
        }

        @Test
        void aBoxInsideTheWorldComesBackAsTheSameInstance() {
            AABB box = new AABB(1, 0, 1, 5, 10, 5);
            for (WorldFold fold : FOLDS) {
                List<AABB> parts = splitAcrossBounds(fold, box);
                assertEquals(1, parts.size(), () -> in(fold));
                assertSame(box, parts.getFirst(), () -> in(fold));
            }
        }

        @Test
        void aBoxStartingOnTheLowerBoundAndWiderThanTheWorldIsStillCutInsideTheBounds() {
            WrapDomain domain = blockX(EVEN);
            AABB box = new AABB(domain.lowerBound, 0, 0, domain.lowerBound + domain.domainLength + 10, 16, 8);
            for (AABB part : splitAcrossBounds(EVEN, box)) {
                assertTrue(part.maxX <= domain.upperBound + 1e-9,
                        () -> "part " + part + " of " + box + " leaves the X bounds " + in(EVEN));
            }
        }
    }

    @Nested
    class SplitRegionAcrossBounds {
        private BoundingBox sampleRegion(Random random, WorldFold fold) {
            int minX = sampleBlockInt(random, blockX(fold));
            int minY = random.nextInt(320) - 64;
            int minZ = sampleBlockInt(random, blockZ(fold));
            return new BoundingBox(
                    minX, minY, minZ,
                    minX + random.nextInt(2 * reachCap(blockX(fold), 16_000) + 1),
                    minY + random.nextInt(32),
                    minZ + random.nextInt(2 * reachCap(blockZ(fold), 16_000) + 1));
        }

        private long coveredCells(WrapDomain domain, int min, int max) {
            long cells = (long) max - min + 1;
            return domain instanceof WrapDomain.Noop ? cells : Math.min(cells, domain.domainLength);
        }

        private long cellCount(BoundingBox region) {
            return ((long) region.maxX() - region.minX() + 1)
                    * (region.maxY() - region.minY() + 1)
                    * (region.maxZ() - region.minZ() + 1);
        }

        private void assertLiesOnACopyInsideTheWorld(WrapDomain domain, int coord, int copy,
                WorldFold fold) {
            if (domain instanceof WrapDomain.Noop) {
                assertEquals(coord, copy, () -> in(fold));
                return;
            }

            assertEquals(0, ((long) copy - coord) % domain.domainLength,
                    () -> copy + " is not a copy of " + coord + " " + in(fold));
            assertFalse(domain.isOver(copy), () -> copy + " lies outside the world " + in(fold));
        }

        @Test
        void partsCoverTheRegionCellsCappedAtOneWorldPerAxis() {
            Random random = new Random(SEED);
            for (WorldFold fold : FOLDS) {
                for (int i = 0; i < SAMPLES; i++) {
                    BoundingBox region = sampleRegion(random, fold);
                    long expected = coveredCells(blockX(fold), region.minX(), region.maxX())
                            * (region.maxY() - region.minY() + 1)
                            * coveredCells(blockZ(fold), region.minZ(), region.maxZ());

                    long total = 0;
                    for (BoundingBox part : splitAcrossBounds(fold, region)) {
                        total += cellCount(part);
                    }

                    long covered = total;
                    assertEquals(expected, total,
                            () -> "splitAcrossBounds(" + region + ") covers " + covered + " cells instead of "
                                    + expected + " " + in(fold));
                }
            }
        }

        @Test
        void everyPartLiesInsideTheBoundsAndKeepsTheRegionHeight() {
            Random random = new Random(SEED);
            for (WorldFold fold : FOLDS) {
                for (int i = 0; i < SAMPLES; i++) {
                    BoundingBox region = sampleRegion(random, fold);
                    for (BoundingBox part : splitAcrossBounds(fold, region)) {
                        assertFalse(blockX(fold).isOver(part.minX()) || blockX(fold).isOver(part.maxX()),
                                () -> "part " + part + " of " + region + " leaves the X bounds " + in(fold));
                        assertFalse(blockZ(fold).isOver(part.minZ()) || blockZ(fold).isOver(part.maxZ()),
                                () -> "part " + part + " of " + region + " leaves the Z bounds " + in(fold));
                        assertEquals(region.minY(), part.minY(), () -> in(fold));
                        assertEquals(region.maxY(), part.maxY(), () -> in(fold));
                    }
                }
            }
        }

        @Test
        void containmentAgreesWithTheLatticeOfRegionCopies() {
            Random random = new Random(SEED);
            for (WorldFold fold : FOLDS) {
                for (int i = 0; i < SAMPLES; i++) {
                    BoundingBox region = sampleRegion(random, fold);
                    List<BoundingBox> parts = splitAcrossBounds(fold, region);
                    for (int p = 0; p < 4; p++) {
                        int x = sampleBlockInt(random, blockX(fold));
                        int z = sampleBlockInt(random, blockZ(fold));
                        boolean expected = containsOnLattice(blockX(fold), region.minX(), region.maxX(), x)
                                && containsOnLattice(blockZ(fold), region.minZ(), region.maxZ(), z);

                        int wrappedX = blockX(fold).wrap(x);
                        int wrappedZ = blockZ(fold).wrap(z);
                        boolean actual = parts.stream()
                                .anyMatch(part -> part.isInside(wrappedX, region.minY(), wrappedZ));

                        assertEquals(expected, actual,
                                () -> "(" + x + ", " + z + ") in splitAcrossBounds(" + region + ") should be "
                                        + expected + " " + in(fold));
                    }
                }
            }
        }

        @Test
        void aSingleBlockNamesTheSamePhysicalBlockFromEveryCopy() {
            Random random = new Random(SEED);
            for (WorldFold fold : FOLDS) {
                for (int i = 0; i < SAMPLES; i++) {
                    int x = sampleBlockInt(random, blockX(fold));
                    int y = random.nextInt(320) - 64;
                    int z = sampleBlockInt(random, blockZ(fold));
                    BoundingBox region = new BoundingBox(x, y, z, x, y, z);

                    List<BoundingBox> parts = splitAcrossBounds(fold, region);
                    assertEquals(1, parts.size(), () -> "single block " + region + " " + in(fold));

                    BoundingBox part = parts.getFirst();
                    assertEquals(1, cellCount(part), () -> "single block " + region + " " + in(fold));
                    assertEquals(y, part.minY(), () -> in(fold));
                    assertLiesOnACopyInsideTheWorld(blockX(fold), x, part.minX(), fold);
                    assertLiesOnACopyInsideTheWorld(blockZ(fold), z, part.minZ(), fold);
                }
            }
        }

        @Test
        void aRegionInsideTheWorldComesBackAsTheSameInstance() {
            BoundingBox region = new BoundingBox(1, 0, 1, 5, 10, 5);
            for (WorldFold fold : FOLDS) {
                List<BoundingBox> parts = splitAcrossBounds(fold, region);
                assertEquals(1, parts.size(), () -> in(fold));
                assertSame(region, parts.getFirst(), () -> in(fold));
            }
        }

        @Test
        void aRegionCrossingTheSeamIsCutInTwoAtTheBounds() {
            WrapDomain domain = blockX(EVEN);
            BoundingBox region = new BoundingBox(domain.upperBound - 4, 0, 0, domain.upperBound + 3, 8, 4);

            List<BoundingBox> parts = splitAcrossBounds(EVEN, region);
            assertEquals(2, parts.size());
            assertEquals(cellCount(region), parts.stream().mapToLong(this::cellCount).sum());
            for (BoundingBox part : parts) {
                assertFalse(domain.isOver(part.minX()) || domain.isOver(part.maxX()),
                        () -> "part " + part + " leaves the X bounds " + in(EVEN));
            }

            assertSame(region, splitAcrossBounds(WorldFolds.NOOP, region).getFirst());
        }
    }

    @Nested
    class RegionFolding {
        private BoundingBox sampleRegion(Random random, WorldFold fold) {
            int minX = sampleBlockInt(random, blockX(fold));
            int minZ = sampleBlockInt(random, blockZ(fold));
            int minY = random.nextInt(320) - 64;
            return new BoundingBox(
                    minX, minY, minZ,
                    minX + random.nextInt(2 * reachCap(blockX(fold), 16_000) + 1),
                    minY + random.nextInt(32),
                    minZ + random.nextInt(2 * reachCap(blockZ(fold), 16_000) + 1));
        }

        private boolean overlapNaive(WorldFold fold, BoundingBox first, BoundingBox second) {
            if (first.minY() > second.maxY() || second.minY() > first.maxY()) return false;
            return axisOverlapOnLattice(blockX(fold), first.minX(), first.maxX(), second.minX(), second.maxX())
                    && axisOverlapOnLattice(blockZ(fold), first.minZ(), first.maxZ(), second.minZ(), second.maxZ());
        }

        private boolean axisOverlapOnLattice(WrapDomain domain, int aMin, int aMax, int bMin, int bMax) {
            for (int laps = -LAPS; laps <= LAPS; laps++) {
                long shift = (long) laps * domain.domainLength;
                if (aMin + shift <= bMax && bMin <= aMax + shift) return true;
            }
            return false;
        }

        @Test
        void regionsOverlapAgreesWithTheLatticeOfWorldCopies() {
            Random random = new Random(SEED);
            for (WorldFold fold : FOLDS) {
                for (int i = 0; i < SAMPLES; i++) {
                    BoundingBox first = sampleRegion(random, fold);
                    BoundingBox second = sampleRegion(random, fold);
                    boolean expected = overlapNaive(fold, first, second);
                    assertEquals(expected, fold.regionsOverlap(first, second),
                            () -> "regionsOverlap(" + first + ", " + second + ") should be " + expected + " "
                                    + in(fold));
                }
            }
        }

        @Test
        void regionsTouchingOnlyThroughTheSeamOverlap() {
            BoundingBox pastTheTop = new BoundingBox(500, 0, 0, 515, 5, 5);
            BoundingBox atTheBottom = new BoundingBox(-512, 0, 0, -505, 5, 5);
            assertTrue(EVEN.regionsOverlap(pastTheTop, atTheBottom));
            assertFalse(WorldFolds.NOOP.regionsOverlap(pastTheTop, atTheBottom));
        }

        private AABB sampleBox(Random random, WorldFold fold) {
            double minX = sampleBlock(random, blockX(fold));
            double minZ = sampleBlock(random, blockZ(fold));
            double minY = sampleY(random);
            return new AABB(
                    minX, minY, minZ,
                    minX + random.nextDouble() * sampleBoxSpan(random, blockX(fold)),
                    minY + random.nextDouble() * 32,
                    minZ + random.nextDouble() * sampleBoxSpan(random, blockZ(fold)));
        }

        private double sampleBoxSpan(Random random, WrapDomain domain) {
            return random.nextBoolean() ? 4.0 : 2 * reachCap(domain, 16_000);
        }

        private boolean boxOverlapNaive(WorldFold fold, AABB first, AABB second) {
            if (first.minY >= second.maxY || second.minY >= first.maxY) return false;
            return axisBoxOverlapOnLattice(blockX(fold), first.minX, first.maxX, second.minX, second.maxX)
                    && axisBoxOverlapOnLattice(blockZ(fold), first.minZ, first.maxZ, second.minZ, second.maxZ);
        }

        private boolean axisBoxOverlapOnLattice(WrapDomain domain, double aMin, double aMax, double bMin, double bMax) {
            for (int laps = -LAPS; laps <= LAPS; laps++) {
                double shift = laps * (double) domain.domainLength;
                if (aMin + shift < bMax && bMin < aMax + shift) return true;
            }
            return false;
        }

        @Test
        void boxesOverlapAgreesWithTheLatticeOfWorldCopies() {
            Random random = new Random(SEED);
            for (WorldFold fold : FOLDS) {
                for (int i = 0; i < SAMPLES; i++) {
                    AABB first = sampleBox(random, fold);
                    AABB second = sampleBox(random, fold);
                    boolean expected = boxOverlapNaive(fold, first, second);
                    assertEquals(expected, fold.boxesOverlap(first, second),
                            () -> "boxesOverlap(" + first + ", " + second + ") should be " + expected + " "
                                    + in(fold));
                }
            }
        }

        @Test
        void aBoxInsideTheBoundsOverlapsTheCopyOfABoxCrossingThem() {
            WrapDomain domain = blockX(EVEN);
            AABB straddler = new AABB(domain.upperBound - 1.19, 0, 0, domain.upperBound + 0.19, 1, 1);
            AABB touching = new AABB(domain.upperBound - 1.0, 0, 0, domain.upperBound, 1, 1);
            AABB query = new AABB(domain.lowerBound, 0, 0, domain.lowerBound + 0.6, 1, 1);
            assertTrue(EVEN.boxesOverlap(query, straddler));
            assertFalse(EVEN.boxesOverlap(query, touching));
            assertFalse(WorldFolds.NOOP.boxesOverlap(query, straddler));
        }
    }

    @Nested
    class UnwrapPairing {
        private void checkUnwrappedAxis(WrapDomain domain, int ref, int wrapped, int unwrapped,
                String axis, WorldFold fold) {
            assertEquals(wrapped, domain.wrap(unwrapped),
                    () -> axis + ": wrap(unwrap(" + ref + ", " + wrapped + ")) changed the position " + in(fold));

            long best = Long.MAX_VALUE;
            for (int laps = -LAPS; laps <= LAPS; laps++) {
                best = Math.min(best, Math.abs(wrapped + (long) laps * domain.domainLength - ref));
            }
            assertEquals(best, Math.abs((long) unwrapped - ref),
                    () -> axis + ": unwrap(" + ref + ", " + wrapped + ") is not the nearest copy " + in(fold));
            if (!(domain instanceof WrapDomain.Noop)) {
                assertTrue(2L * Math.abs((long) unwrapped - ref) <= domain.domainLength,
                        () -> axis + ": unwrap(" + ref + ", " + wrapped + ") is over half a world away " + in(fold));
            }
        }

        @Test
        void chunkUnwrapPairsWithWrapAndLandsOnTheNearestCopy() {
            Random random = new Random(SEED);
            for (WorldFold fold : FOLDS) {
                for (int i = 0; i < SAMPLES; i++) {
                    ChunkPos ref = new ChunkPos(
                            sampleChunk(random, chunkX(fold)),
                            sampleChunk(random, chunkZ(fold)));
                    ChunkPos wrapped = fold.fold(new ChunkPos(
                            sampleChunk(random, chunkX(fold)),
                            sampleChunk(random, chunkZ(fold))));

                    ChunkPos unwrapped = fold.nearestCopy(ref, wrapped);

                    assertEquals(wrapped, fold.fold(unwrapped),
                            () -> "Chunk.wrap(unwrap(" + ref + ", " + wrapped + ")) " + in(fold));
                    checkUnwrappedAxis(chunkX(fold), ref.x(), wrapped.x(), unwrapped.x(), "chunk X", fold);
                    checkUnwrappedAxis(chunkZ(fold), ref.z(), wrapped.z(), unwrapped.z(), "chunk Z", fold);
                }
            }
        }

        @Test
        void blockUnwrapPairsWithWrapKeepsYAndLandsOnTheNearestCopy() {
            Random random = new Random(SEED);
            for (WorldFold fold : FOLDS) {
                for (int i = 0; i < SAMPLES; i++) {
                    BlockPos ref = new BlockPos(
                            sampleBlockInt(random, blockX(fold)),
                            random.nextInt(384) - 64,
                            sampleBlockInt(random, blockZ(fold)));
                    BlockPos wrapped = fold.fold(new BlockPos(
                            sampleBlockInt(random, blockX(fold)),
                            random.nextInt(384) - 64,
                            sampleBlockInt(random, blockZ(fold))));

                    BlockPos unwrapped = fold.nearestCopy(ref, wrapped);

                    assertEquals(wrapped.getY(), unwrapped.getY(),
                            () -> "Block.unwrap moved Y " + in(fold));
                    assertEquals(wrapped, fold.fold(unwrapped),
                            () -> "Block.wrap(unwrap(" + ref + ", " + wrapped + ")) " + in(fold));
                    checkUnwrappedAxis(blockX(fold), ref.getX(), wrapped.getX(), unwrapped.getX(),
                            "block X", fold);
                    checkUnwrappedAxis(blockZ(fold), ref.getZ(), wrapped.getZ(), unwrapped.getZ(),
                            "block Z", fold);
                }
            }
        }
    }

    @Nested
    class NodesAndViewDistance {
        @Test
        void wrapBlockNodeWrapsTheHorizontalsAndKeepsY() {
            Random random = new Random(SEED);
            for (WorldFold fold : FOLDS) {
                for (int i = 0; i < SAMPLES; i++) {
                    int x = sampleBlockInt(random, blockX(fold));
                    int y = random.nextInt(384) - 64;
                    int z = sampleBlockInt(random, blockZ(fold));

                    long wrapped = fold.foldBlockNode(BlockPos.asLong(x, y, z));

                    assertEquals(blockX(fold).wrap(x), BlockPos.getX(wrapped),
                            () -> "wrapBlockNode X of (" + x + ", " + y + ", " + z + ") " + in(fold));
                    assertEquals(y, BlockPos.getY(wrapped),
                            () -> "wrapBlockNode moved Y of (" + x + ", " + y + ", " + z + ") " + in(fold));
                    assertEquals(blockZ(fold).wrap(z), BlockPos.getZ(wrapped),
                            () -> "wrapBlockNode Z of (" + x + ", " + y + ", " + z + ") " + in(fold));
                }
            }
        }

        @Test
        void anInBoundsNodeComesBackUnchanged() {
            long node = BlockPos.asLong(5, 70, 7);
            for (WorldFold fold : FOLDS) {
                assertEquals(node, fold.foldBlockNode(node), () -> in(fold));
            }
        }

        @Test
        void viewDistanceIsClampedOnlyWhenItExceedsTheCap() {
            assertEquals(29, EVEN.limitViewDistance(32));
            assertEquals(8, EVEN.limitViewDistance(8));
            assertEquals(29, X_ONLY.limitViewDistance(64));
            assertEquals(32, WorldFolds.NOOP.limitViewDistance(32));
        }
    }
}
