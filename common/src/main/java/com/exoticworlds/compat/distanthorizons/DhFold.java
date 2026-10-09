package com.exoticworlds.compat.distanthorizons;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.core.CoordinateConstants;

import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;

public final class DhFold {
    private static final int SNAP_CELLS_PER_WORLD = 16;
    private static final long NO_PERIOD = 0L;
    private static final int INVERSE_NEWTON_STEPS = 5;

    public record Period(long xBlocks, long zBlocks, long xShiftBlocks) {
    }

    public record Section(int x, int z) {
    }

    public static int sectionWidthBlocks(byte detailLevel) {
        return 1 << detailLevel;
    }

    public static byte maxExactDetailLevel(DhLattice lattice) {
        ToroidalShape shape = lattice.shape();
        int cap = Byte.MAX_VALUE;
        for (Direction.Axis axis : CoordinateConstants.HORIZONTAL_AXES) {
            if (shape.loops(axis)) {
                cap = Math.min(cap, Integer.numberOfTrailingZeros(shape.widthBlocks(axis)));
                cap = Math.min(cap, Integer.numberOfTrailingZeros(shape.minBlock(axis)));
            }
        }

        if (lattice.isSkewed()) {
            cap = Math.min(cap, Integer.numberOfTrailingZeros(lattice.skewBlocks()));
        }

        return (byte) cap;
    }

    public static byte maxRenderableDetailLevel(DhLattice lattice, byte leafDetailLevel) {
        return (byte) Math.max(maxExactDetailLevel(lattice), leafDetailLevel);
    }

    public static byte maxExpectedDetailLevel(DhLattice lattice, byte leafDetailLevel) {
        return (byte) (maxRenderableDetailLevel(lattice, leafDetailLevel) - leafDetailLevel);
    }

    public static long periodBlocks(ToroidalShape shape, Direction.Axis axis, byte detailLevel) {
        return alignedPeriodBlocks(shape.widthBlocks(axis), detailLevel);
    }

    public static int periodSections(ToroidalShape shape, Direction.Axis axis, byte detailLevel) {
        return (int) (periodBlocks(shape, axis, detailLevel) / sectionWidthBlocks(detailLevel));
    }

    public static Period period(DhLattice lattice, byte detailLevel) {
        ToroidalShape shape = lattice.shape();
        long x = shape.loops(Direction.Axis.X)
                ? alignedPeriodBlocks(shape.widthBlocks(Direction.Axis.X), detailLevel)
                : NO_PERIOD;
        if (!shape.loops(Direction.Axis.Z)) {
            return new Period(x, NO_PERIOD, 0L);
        }

        int widthZ = shape.widthBlocks(Direction.Axis.Z);
        int lapsExponent = detailLevel - Math.min(detailLevel, Integer.numberOfTrailingZeros(widthZ));
        if (!lattice.isSkewed()) {
            return new Period(x, ((long) widthZ) << lapsExponent, 0L);
        }

        int widthX = shape.widthBlocks(Direction.Axis.X);
        int alignX = Math.min(detailLevel, Integer.numberOfTrailingZeros(widthX));
        lapsExponent = Math.max(lapsExponent, alignX - Integer.numberOfTrailingZeros(lattice.skewBlocks()));
        long rowShift = ((long) lattice.skewBlocks()) << lapsExponent;
        return new Period(x, ((long) widthZ) << lapsExponent, alignedShift(widthX, detailLevel, rowShift, x));
    }

    private static long alignedPeriodBlocks(int worldWidth, byte detailLevel) {
        int gcd = 1 << Math.min(Integer.numberOfTrailingZeros(worldWidth), detailLevel);
        return (long) (worldWidth / gcd) * sectionWidthBlocks(detailLevel);
    }

    private static long alignedShift(int widthX, byte detailLevel, long rowShift, long periodX) {
        int alignX = Integer.numberOfTrailingZeros(widthX);
        if (alignX >= detailLevel) {
            return Math.floorMod(rowShift, periodX);
        }

        long oddWidth = widthX >> alignX;
        long mask = (1L << (detailLevel - alignX)) - 1;
        long laps = (-(rowShift >> alignX) * inverseOfOdd(oddWidth)) & mask;
        return Math.floorMod(rowShift + laps * widthX, periodX);
    }

    private static long inverseOfOdd(long odd) {
        long inverse = odd;
        for (int step = 0; step < INVERSE_NEWTON_STEPS; step++) {
            inverse *= 2 - odd * inverse;
        }

        return inverse;
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

    public static int foldBlock(ToroidalShape shape, Direction.Axis axis, byte leafDetailLevel, int block) {
        if (!shape.loops(axis)) {
            return block;
        }

        long period = periodBlocks(shape, axis, leafDetailLevel);
        return (int) (block - Math.floorDiv((long) block - shape.minBlock(axis), period) * period);
    }

    private static int foldX(DhLattice lattice, byte detailLevel, int unitBlocks, int valueX, int valueZ) {
        ToroidalShape shape = lattice.shape();
        Period period = period(lattice, detailLevel);
        long blockX = (long) valueX * unitBlocks;
        if (period.xShiftBlocks() != 0L) {
            long blockZ = (long) valueZ * unitBlocks;
            long lapsZ = Math.floorDiv(blockZ - shape.minBlock(Direction.Axis.Z), period.zBlocks());
            blockX -= lapsZ * period.xShiftBlocks();
        }

        if (shape.loops(Direction.Axis.X)) {
            blockX -= Math.floorDiv(blockX - shape.minBlock(Direction.Axis.X), period.xBlocks()) * period.xBlocks();
        }

        return (int) (blockX / unitBlocks);
    }

    private static int foldZ(DhLattice lattice, byte detailLevel, int unitBlocks, int valueZ) {
        ToroidalShape shape = lattice.shape();
        if (!shape.loops(Direction.Axis.Z)) {
            return valueZ;
        }

        long period = period(lattice, detailLevel).zBlocks();
        long laps = Math.floorDiv((long) valueZ * unitBlocks - shape.minBlock(Direction.Axis.Z), period);
        return (int) (valueZ - laps * (period / unitBlocks));
    }

    public static boolean isCompleteSection(DhLattice lattice, byte leafDetailLevel, byte detailLevel, int sectionX,
            int sectionZ) {
        return detailLevel <= leafDetailLevel || foldedSpanInsideTheWorld(lattice, detailLevel, sectionX, sectionZ);
    }

    public static boolean foldedSpanInsideTheWorld(DhLattice lattice, byte detailLevel, int sectionX, int sectionZ) {
        return spanInside(lattice.shape(), Direction.Axis.X, detailLevel,
                foldSectionX(lattice, detailLevel, sectionX, sectionZ))
                && spanInside(lattice.shape(), Direction.Axis.Z, detailLevel,
                        foldSectionZ(lattice, detailLevel, sectionZ));
    }

    private static boolean spanInside(ToroidalShape shape, Direction.Axis axis, byte detailLevel, int foldedSection) {
        if (!shape.loops(axis)) {
            return true;
        }

        int width = sectionWidthBlocks(detailLevel);
        return (long) foldedSection * width + width <= shape.maxBlock(axis);
    }

    public static boolean containsACopy(DhLattice lattice, byte detailLevel, int sectionX, int sectionZ,
            byte copyDetailLevel, int copySectionX, int copySectionZ) {
        ToroidalShape shape = lattice.shape();
        long width = sectionWidthBlocks(detailLevel);
        long cornerX = (long) sectionX * width;
        long cornerZ = (long) sectionZ * width;
        long copyX = (long) copySectionX * sectionWidthBlocks(copyDetailLevel);
        long copyZ = (long) copySectionZ * sectionWidthBlocks(copyDetailLevel);
        Period period = period(lattice, copyDetailLevel);
        if (!shape.loops(Direction.Axis.Z)) {
            return copyZ >= cornerZ && copyZ < cornerZ + width && copyXInside(shape, period, cornerX, width, copyX);
        }

        long firstLap = Math.ceilDiv(cornerZ - copyZ, period.zBlocks());
        long laps = Math.ceilDiv(cornerZ + width - copyZ, period.zBlocks()) - firstLap;
        long distinctShifts = shape.loops(Direction.Axis.X) && period.xShiftBlocks() != 0L
                ? period.xBlocks() / gcd(period.xBlocks(), period.xShiftBlocks())
                : 1L;
        for (long lap = firstLap; lap < firstLap + Math.min(laps, distinctShifts); lap++) {
            if (copyXInside(shape, period, cornerX, width, copyX + lap * period.xShiftBlocks())) {
                return true;
            }
        }

        return false;
    }

    private static boolean copyXInside(ToroidalShape shape, Period period, long cornerX, long width, long copyX) {
        if (shape.loops(Direction.Axis.X)) {
            copyX += Math.ceilDiv(cornerX - copyX, period.xBlocks()) * period.xBlocks();
        }

        return cornerX <= copyX && copyX < cornerX + width;
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
        ToroidalShape shape = lattice.shape();
        int narrowest = Integer.MAX_VALUE;
        for (Direction.Axis axis : CoordinateConstants.HORIZONTAL_AXES) {
            if (shape.loops(axis)) {
                narrowest = Math.min(narrowest, shape.widthBlocks(axis));
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
        ToroidalShape shape = lattice.shape();
        Period period = period(lattice, detailLevel);
        int width = sectionWidthBlocks(detailLevel);
        long x = sectionX;
        long z = sectionZ;
        if (shape.loops(Direction.Axis.Z)) {
            long laps = lapsToward(period.zBlocks(), windowCentreBlock(snapLevel, detailLevel, z) - refBlockZ);
            z -= laps * (period.zBlocks() / width);
            x -= laps * (period.xShiftBlocks() / width);
        }

        if (shape.loops(Direction.Axis.X)) {
            long laps = lapsToward(period.xBlocks(), windowCentreBlock(snapLevel, detailLevel, x) - refBlockX);
            x -= laps * (period.xBlocks() / width);
        }

        return new Section((int) x, (int) z);
    }

    private static long windowCentreBlock(byte snapLevel, byte detailLevel, long section) {
        return detailLevel <= snapLevel
                ? sectionCentreBlock(snapLevel, section >> (snapLevel - detailLevel))
                : sectionCentreBlock(detailLevel, section);
    }

    public static boolean isNearestSection(DhLattice lattice, Direction.Axis axis, byte snapLevel,
            byte detailLevel, int refBlock, int section) {
        ToroidalShape shape = lattice.shape();
        if (!shape.loops(axis)) {
            return true;
        }

        long worldWidth = shape.widthBlocks(axis);
        if (detailLevel <= snapLevel) {
            return lapsToward(worldWidth, windowCentreBlock(snapLevel, detailLevel, section) - refBlock) == 0;
        }

        long first = sectionCentreBlock(snapLevel, firstCell(snapLevel, detailLevel, section));
        long last = sectionCentreBlock(snapLevel, lastCell(snapLevel, detailLevel, section));
        return lapsToward(worldWidth, first - refBlock) == 0 && lapsToward(worldWidth, last - refBlock) == 0;
    }

    public static boolean overlapsNearestWindow(DhLattice lattice, Direction.Axis axis, byte snapLevel,
            byte detailLevel, int refBlock, int section) {
        ToroidalShape shape = lattice.shape();
        if (!shape.loops(axis)) {
            return true;
        }

        if (detailLevel <= snapLevel) {
            return isNearestSection(lattice, axis, snapLevel, detailLevel, refBlock, section);
        }

        long worldWidth = shape.widthBlocks(axis);
        long first = sectionCentreBlock(snapLevel, firstCell(snapLevel, detailLevel, section));
        long last = sectionCentreBlock(snapLevel, lastCell(snapLevel, detailLevel, section));
        return lapsToward(worldWidth, first - refBlock) <= 0 && lapsToward(worldWidth, last - refBlock) >= 0;
    }

    private static long firstCell(byte snapLevel, byte detailLevel, int section) {
        return (long) section << (detailLevel - snapLevel);
    }

    private static long lastCell(byte snapLevel, byte detailLevel, int section) {
        return firstCell(snapLevel, detailLevel, section) + (1L << (detailLevel - snapLevel)) - 1;
    }

    private static long sectionCentreBlock(byte detailLevel, long section) {
        int width = sectionWidthBlocks(detailLevel);
        return section * width + width / 2;
    }

    private static long lapsToward(long worldWidth, long delta) {
        return Math.floorDiv(2 * delta - 1 + worldWidth, 2 * worldWidth);
    }

    public static long seamChebyshevDistance(DhLattice lattice, long fromX, long fromZ, long toX, long toZ) {
        ToroidalShape shape = lattice.shape();
        long deltaX = toX - fromX;
        long deltaZ = toZ - fromZ;
        if (!shape.loops(Direction.Axis.Z)) {
            return Math.max(axisDistance(shape, deltaX), Math.abs(deltaZ));
        }

        long widthZ = shape.widthBlocks(Direction.Axis.Z);
        long below = Math.floorDiv(deltaZ, widthZ);
        long reach = rowsThatCanWin(lattice);
        long nearest = Long.MAX_VALUE;
        for (long row = below - reach; row <= below + 1 + reach; row++) {
            long distance = Math.max(axisDistance(shape, deltaX - row * lattice.skewBlocks()),
                    Math.abs(deltaZ - row * widthZ));
            nearest = Math.min(nearest, distance);
        }

        return nearest;
    }

    static long rowsThatCanWin(DhLattice lattice) {
        if (!lattice.isSkewed()) {
            return 0L;
        }

        ToroidalShape shape = lattice.shape();
        return Math.ceilDiv(shape.widthBlocks(Direction.Axis.X), 2L * shape.widthBlocks(Direction.Axis.Z));
    }

    private static long axisDistance(ToroidalShape shape, long deltaX) {
        if (!shape.loops(Direction.Axis.X)) {
            return Math.abs(deltaX);
        }

        long width = shape.widthBlocks(Direction.Axis.X);
        long lapped = Math.floorMod(deltaX, width);
        return Math.min(lapped, width - lapped);
    }

    public static boolean overlapsNearestLap(DhLattice lattice, Direction.Axis axis, int refBlock, int minBlock,
            int widthBlocks) {
        ToroidalShape shape = lattice.shape();
        if (!shape.loops(axis)) {
            return true;
        }

        long half = shape.widthBlocks(axis) / 2L;
        return minBlock < refBlock + half && minBlock + (long) widthBlocks > refBlock - half;
    }

    private DhFold() {
    }
}
