package com.exoticworlds.compat.distanthorizons;

import com.exoticworlds.compat.lod.AlignedPeriod;
import com.exoticworlds.core.world.CoordinateConstants;
import com.exoticworlds.core.fold.WorldFold;

import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;

public final class DhFold {
    private static final int SNAP_CELLS_PER_WORLD = 16;

    public record Section(int x, int z) {
    }

    public static int sectionWidthBlocks(byte detailLevel) {
        return 1 << detailLevel;
    }

    public static long periodBlocks(WorldFold fold, Direction.Axis axis, byte detailLevel) {
        return AlignedPeriod.blocks(fold.getWidthBlocks(axis), detailLevel);
    }

    public static int periodSections(WorldFold fold, Direction.Axis axis, byte detailLevel) {
        return (int) (periodBlocks(fold, axis, detailLevel) / sectionWidthBlocks(detailLevel));
    }

    public static AlignedPeriod period(DhLattice lattice, byte detailLevel) {
        return AlignedPeriod.of(lattice.fold(), lattice.skewBlocks(), detailLevel);
    }

    public static int foldSectionX(DhLattice lattice, byte detailLevel, int sectionX, int sectionZ) {
        return foldX(lattice, detailLevel, sectionWidthBlocks(detailLevel), sectionX, sectionZ);
    }

    public static int foldSectionZ(DhLattice lattice, byte detailLevel, int sectionZ) {
        return foldZ(lattice, detailLevel, sectionWidthBlocks(detailLevel), sectionZ);
    }

    public static int foldChunkX(DhLattice lattice, byte leafDetailLevel, int chunkX, int chunkZ) {
        return foldX(lattice, leafDetailLevel, SectionPos.SECTION_SIZE, chunkX, chunkZ);
    }

    public static int foldChunkZ(DhLattice lattice, byte leafDetailLevel, int chunkZ) {
        return foldZ(lattice, leafDetailLevel, SectionPos.SECTION_SIZE, chunkZ);
    }

    private static int foldX(DhLattice lattice, byte detailLevel, int unitBlocks, int valueX, int valueZ) {
        long blockX = period(lattice, detailLevel).foldX(lattice.fold(), (long) valueX * unitBlocks,
                (long) valueZ * unitBlocks);
        return (int) (blockX / unitBlocks);
    }

    private static int foldZ(DhLattice lattice, byte detailLevel, int unitBlocks, int valueZ) {
        long blockZ = period(lattice, detailLevel).foldZ(lattice.fold(), (long) valueZ * unitBlocks);
        return (int) (blockZ / unitBlocks);
    }

    public static boolean isCompleteSection(DhLattice lattice, byte leafDetailLevel, byte detailLevel, int sectionX,
            int sectionZ) {
        return detailLevel <= leafDetailLevel || isFoldedSpanInsideTheWorld(lattice, detailLevel, sectionX, sectionZ);
    }

    public static boolean isFoldedSpanInsideTheWorld(DhLattice lattice, byte detailLevel, int sectionX, int sectionZ) {
        return isSpanInside(lattice.fold(), Direction.Axis.X, detailLevel,
                foldSectionX(lattice, detailLevel, sectionX, sectionZ))
                && isSpanInside(lattice.fold(), Direction.Axis.Z, detailLevel,
                        foldSectionZ(lattice, detailLevel, sectionZ));
    }

    private static boolean isSpanInside(WorldFold fold, Direction.Axis axis, byte detailLevel, int foldedSection) {
        if (!fold.isLooped(axis)) {
            return true;
        }

        int widthBlocks = sectionWidthBlocks(detailLevel);
        return (long) foldedSection * widthBlocks + widthBlocks <= fold.getMaxBlock(axis);
    }

    public static boolean isContainingACopy(DhLattice lattice, byte detailLevel, int sectionX, int sectionZ,
            byte copyDetailLevel, int copySectionX, int copySectionZ) {
        WorldFold fold = lattice.fold();
        long widthBlocks = sectionWidthBlocks(detailLevel);
        long cornerX = (long) sectionX * widthBlocks;
        long cornerZ = (long) sectionZ * widthBlocks;
        long copyX = (long) copySectionX * sectionWidthBlocks(copyDetailLevel);
        long copyZ = (long) copySectionZ * sectionWidthBlocks(copyDetailLevel);
        AlignedPeriod period = period(lattice, copyDetailLevel);
        if (!fold.isLooped(Direction.Axis.Z)) {
            return copyZ >= cornerZ && copyZ < cornerZ + widthBlocks && isCopyXInside(fold, period, cornerX, widthBlocks, copyX);
        }

        long firstLap = Math.ceilDiv(cornerZ - copyZ, period.zBlocks());
        long laps = Math.ceilDiv(cornerZ + widthBlocks - copyZ, period.zBlocks()) - firstLap;
        long distinctShifts = fold.isLooped(Direction.Axis.X) && period.xShiftBlocks() != 0L
                ? period.xBlocks() / gcd(period.xBlocks(), period.xShiftBlocks())
                : 1L;
        for (long lap = firstLap; lap < firstLap + Math.min(laps, distinctShifts); lap++) {
            if (isCopyXInside(fold, period, cornerX, widthBlocks, copyX + lap * period.xShiftBlocks())) {
                return true;
            }
        }

        return false;
    }

    private static boolean isCopyXInside(WorldFold fold, AlignedPeriod period, long cornerX, long widthBlocks,
            long copyX) {
        if (fold.isLooped(Direction.Axis.X)) {
            copyX += Math.ceilDiv(cornerX - copyX, period.xBlocks()) * period.xBlocks();
        }

        return cornerX <= copyX && copyX < cornerX + widthBlocks;
    }

    private static long gcd(long first, long second) {
        long a = Math.abs(first);
        long b = Math.abs(second);
        while (b != 0L) {
            long remainder = a % b;
            a = b;
            b = remainder;
        }

        return a;
    }

    public static byte snapDetailLevel(DhLattice lattice, byte leafDetailLevel) {
        WorldFold fold = lattice.fold();
        int narrowest = Integer.MAX_VALUE;
        for (Direction.Axis axis : CoordinateConstants.HORIZONTAL_AXES) {
            if (fold.isLooped(axis)) {
                narrowest = Math.min(narrowest, fold.getWidthBlocks(axis));
            }
        }

        if (narrowest == Integer.MAX_VALUE) {
            return leafDetailLevel;
        }

        int cell = narrowest / SNAP_CELLS_PER_WORLD;
        int level = cell <= 0 ? 0 : Integer.SIZE - 1 - Integer.numberOfLeadingZeros(cell);
        return (byte) Math.max(level, leafDetailLevel);
    }

    public static Section nearestSection(DhLattice lattice, byte snapLevel, byte detailLevel, int refBlockX,
            int refBlockZ, int sectionX, int sectionZ) {
        WorldFold fold = lattice.fold();
        AlignedPeriod period = period(lattice, detailLevel);
        int widthBlocks = sectionWidthBlocks(detailLevel);
        long x = sectionX;
        long z = sectionZ;
        if (fold.isLooped(Direction.Axis.Z)) {
            long laps = lapsToward(period.zBlocks(), windowCenterBlock(snapLevel, detailLevel, z) - refBlockZ);
            z -= laps * (period.zBlocks() / widthBlocks);
            x -= laps * (period.xShiftBlocks() / widthBlocks);
        }

        if (fold.isLooped(Direction.Axis.X)) {
            long laps = lapsToward(period.xBlocks(), windowCenterBlock(snapLevel, detailLevel, x) - refBlockX);
            x -= laps * (period.xBlocks() / widthBlocks);
        }

        return new Section((int) x, (int) z);
    }

    private static long windowCenterBlock(byte snapLevel, byte detailLevel, long section) {
        return detailLevel <= snapLevel
                ? sectionCenterBlock(snapLevel, section >> (snapLevel - detailLevel))
                : sectionCenterBlock(detailLevel, section);
    }

    public static boolean isNearestSection(DhLattice lattice, Direction.Axis axis, byte snapLevel,
            byte detailLevel, int refBlock, int section) {
        WorldFold fold = lattice.fold();
        if (!fold.isLooped(axis)) {
            return true;
        }

        long worldWidthBlocks = fold.getWidthBlocks(axis);
        if (detailLevel <= snapLevel) {
            return lapsToward(worldWidthBlocks, windowCenterBlock(snapLevel, detailLevel, section) - refBlock) == 0;
        }

        long first = sectionCenterBlock(snapLevel, firstCell(snapLevel, detailLevel, section));
        long last = sectionCenterBlock(snapLevel, lastCell(snapLevel, detailLevel, section));
        return lapsToward(worldWidthBlocks, first - refBlock) == 0 && lapsToward(worldWidthBlocks, last - refBlock) == 0;
    }

    public static boolean isOverlappingNearestWindow(DhLattice lattice, Direction.Axis axis, byte snapLevel,
            byte detailLevel, int refBlock, int section) {
        WorldFold fold = lattice.fold();
        if (!fold.isLooped(axis)) {
            return true;
        }

        if (detailLevel <= snapLevel) {
            return isNearestSection(lattice, axis, snapLevel, detailLevel, refBlock, section);
        }

        long worldWidthBlocks = fold.getWidthBlocks(axis);
        long first = sectionCenterBlock(snapLevel, firstCell(snapLevel, detailLevel, section));
        long last = sectionCenterBlock(snapLevel, lastCell(snapLevel, detailLevel, section));
        return lapsToward(worldWidthBlocks, first - refBlock) <= 0 && lapsToward(worldWidthBlocks, last - refBlock) >= 0;
    }

    private static long firstCell(byte snapLevel, byte detailLevel, int section) {
        return (long) section << (detailLevel - snapLevel);
    }

    private static long lastCell(byte snapLevel, byte detailLevel, int section) {
        return firstCell(snapLevel, detailLevel, section) + (1L << (detailLevel - snapLevel)) - 1;
    }

    private static long sectionCenterBlock(byte detailLevel, long section) {
        int widthBlocks = sectionWidthBlocks(detailLevel);
        return section * widthBlocks + widthBlocks / 2;
    }

    private static long lapsToward(long worldWidthBlocks, long delta) {
        return Math.floorDiv(2 * delta - 1 + worldWidthBlocks, 2 * worldWidthBlocks);
    }

    public static long seamChebyshevDistanceBlocks(DhLattice lattice, long fromX, long fromZ, long toX, long toZ) {
        WorldFold fold = lattice.fold();
        long deltaX = toX - fromX;
        long deltaZ = toZ - fromZ;
        if (!fold.isLooped(Direction.Axis.Z)) {
            return Math.max(axisDistanceBlocks(fold, deltaX), Math.abs(deltaZ));
        }

        long widthZ = fold.getWidthBlocks(Direction.Axis.Z);
        long below = Math.floorDiv(deltaZ, widthZ);
        long reach = rowsThatCanWin(fold, lattice.skewBlocks());
        long nearest = Long.MAX_VALUE;
        for (long row = below - reach; row <= below + 1 + reach; row++) {
            long distanceBlocks = Math.max(axisDistanceBlocks(fold, deltaX - row * lattice.skewBlocks()),
                    Math.abs(deltaZ - row * widthZ));
            nearest = Math.min(nearest, distanceBlocks);
        }

        return nearest;
    }

    static long rowsThatCanWin(WorldFold fold, int skewBlocks) {
        if (skewBlocks == 0) {
            return 0L;
        }

        return Math.ceilDiv(fold.getWidthBlocks(Direction.Axis.X), 2L * fold.getWidthBlocks(Direction.Axis.Z));
    }

    private static long axisDistanceBlocks(WorldFold fold, long deltaX) {
        if (!fold.isLooped(Direction.Axis.X)) {
            return Math.abs(deltaX);
        }

        long widthBlocks = fold.getWidthBlocks(Direction.Axis.X);
        long lapped = Math.floorMod(deltaX, widthBlocks);
        return Math.min(lapped, widthBlocks - lapped);
    }

    public static boolean isOverlappingNearestLap(DhLattice lattice, Direction.Axis axis, int refBlock, int minBlock,
            int widthBlocks) {
        WorldFold fold = lattice.fold();
        if (!fold.isLooped(axis)) {
            return true;
        }

        long half = fold.getWidthBlocks(axis) / 2L;
        return minBlock < refBlock + half && minBlock + (long) widthBlocks > refBlock - half;
    }

    private DhFold() {
    }
}
