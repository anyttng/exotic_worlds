package com.exoticworlds.engine.noise.periodic;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.fold.TranslationLattice;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WrapDomain;
import com.exoticworlds.engine.noise.GenerationFoldContext.Context;
import com.exoticworlds.engine.noise.SlotAxes;
import com.exoticworlds.engine.noise.SlotAxis;

import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

public final class PeriodicNoiseSampler {
    static final int[][] GRADIENT = {
            {1, 1, 0},
            {-1, 1, 0},
            {1, -1, 0},
            {-1, -1, 0},
            {1, 0, 1},
            {-1, 0, 1},
            {1, 0, -1},
            {-1, 0, -1},
            {0, 1, 1},
            {0, -1, 1},
            {0, 1, -1},
            {0, -1, -1},
            {1, 1, 0},
            {0, -1, 1},
            {-1, 1, 0},
            {0, -1, -1}
    };

    public static final long UNBOUNDED_PERIOD = 0L;

    static final long HELD_PERIOD = -1L;

    static final long MAX_INDEXED_PERIOD = 1L << 36;

    private static final int X_SLOT = 0;
    private static final int Y_SLOT = 1;
    private static final int Z_SLOT = 2;
    private static final int NO_SLOT = -1;

    private static final int PERMUTATION_MASK = 0xFF;
    private static final int PAIR_SHIFT = 8;

    public static PeriodicLattice lattice(byte[] permutations, double xOffset, double yOffset, double zOffset,
            WorldFold fold, Context context, LapFloor floor) {
        TranslationLattice translations = fold.getBlockLattice();
        SlotAxes axes = context.getSlotAxes();
        double scale = context.getHorizontalScale();
        boolean slotted = axes != SlotAxes.DEFAULT || context.getXDivisor() != 1.0 || context.getZDivisor() != 1.0;
        return slotted
                ? slottedLattice(permutations, xOffset, yOffset, zOffset, translations, axes, context, scale, floor)
                : worldLattice(permutations, xOffset, yOffset, zOffset, translations, context, scale, floor);
    }

    private static PeriodicLattice slottedLattice(byte[] permutations, double xOffset, double yOffset,
            double zOffset, TranslationLattice translations, SlotAxes axes, Context context, double scale,
            LapFloor floor) {
        PeriodicLattice.Axis xAxis = slotAxis(axes.x(), translations, context, scale, floor);
        PeriodicLattice.Axis yAxis = slotAxis(axes.y(), translations, context, scale, floor);
        PeriodicLattice.Axis zAxis = slotAxis(axes.z(), translations, context, scale, floor);
        return new PeriodicLattice(permutations, xOffset, yOffset, zOffset, xAxis, yAxis, zAxis, 1.0, 0.0,
                slotShear(translations, axes, xAxis, yAxis, zAxis));
    }

    private static PeriodicLattice worldLattice(byte[] permutations, double xOffset, double yOffset,
            double zOffset, TranslationLattice translations, Context context, double scale, LapFloor floor) {
        WrapDomain xDomain = translations.getX();
        WrapDomain zDomain = translations.getZ();
        long xPeriod = period(xDomain, scale, floor);
        long zPeriod = period(zDomain, scale, floor);
        double verticalShare = context.verticalShare();
        double correction = OctaveVarianceCorrection.factor(xDomain, zDomain, scale, verticalShare);
        double anchorGain = OctaveVarianceCorrection.anchorGain(xDomain, zDomain, scale, verticalShare);
        PeriodicLattice.Axis xAxis = PeriodicLattice.Axis.of(true, xDomain, xPeriod, scale);
        PeriodicLattice.Axis zAxis = PeriodicLattice.Axis.of(true, zDomain, zPeriod, scale);
        if (translations.isSkewed()) {
            PeriodicLattice.Shear shear = PeriodicLattice.Shear.of(translations, X_SLOT, Z_SLOT, xPeriod, zPeriod);
            double anchor = anchorGain > 0.0
                    ? anchorGain * sample(new PeriodicLattice(permutations, xOffset, yOffset, zOffset,
                            xAxis, PeriodicLattice.Axis.PASS_THROUGH, zAxis, 1.0, 0.0, shear), 0.0, 0.0, 0.0, 0.0, 0.0)
                    : 0.0;
            return new PeriodicLattice(permutations, xOffset, yOffset, zOffset,
                    xAxis, PeriodicLattice.Axis.PASS_THROUGH, zAxis, correction, anchor, shear);
        }

        double anchor = anchorGain > 0.0
                ? anchorGain * anchorSample(permutations, xDomain, zDomain, xPeriod, zPeriod, scale, xOffset, yOffset,
                        zOffset)
                : 0.0;
        return new PeriodicLattice(permutations, xOffset, yOffset, zOffset,
                xAxis, PeriodicLattice.Axis.PASS_THROUGH, zAxis, correction, anchor, null);
    }

    // A slot carrying no world axis arrives already scaled by its caller, so scaling it again would move the lattice.
    private static PeriodicLattice.Axis slotAxis(SlotAxis axis, TranslationLattice translations, Context context,
            double scale, LapFloor floor) {
        WrapDomain domain = axis.getDomain(translations);
        double slotScale = scale / axis.getDivisor(context);
        return PeriodicLattice.Axis.of(axis.hasWorldAxis(), domain, period(domain, slotScale, floor), slotScale);
    }

    private static PeriodicLattice.@Nullable Shear slotShear(TranslationLattice translations, SlotAxes axes,
            PeriodicLattice.Axis... slotAxes) {
        if (!translations.isSkewed()) {
            return null;
        }

        int xSlot = slotOf(axes, SlotAxis.X);
        int zSlot = slotOf(axes, SlotAxis.Z);
        if (xSlot < 0 || zSlot < 0) {
            throw new IllegalStateException("A skewed lattice closes only on a frame carrying both world axes, got "
                    + axes);
        }

        return PeriodicLattice.Shear.of(translations, xSlot, zSlot, slotAxes[xSlot].period(),
                slotAxes[zSlot].period());
    }

    private static int slotOf(SlotAxes axes, SlotAxis axis) {
        if (axes.x() == axis) {
            return X_SLOT;
        }

        if (axes.y() == axis) {
            return Y_SLOT;
        }

        return axes.z() == axis ? Z_SLOT : NO_SLOT;
    }

    public static double sample(PeriodicLattice lattice, double x, double y, double z, double yScale, double yFudge) {
        PeriodicLattice.Shear shear = lattice.shear();
        if (shear != null) {
            return sampleSheared(lattice, shear, x, y, z, yScale, yFudge);
        }

        double xs = lattice.x().coord(x) + lattice.xOffset();
        double ys = lattice.y().coord(y) + lattice.yOffset();
        double zs = lattice.z().coord(z) + lattice.zOffset();
        long xCell = Mth.lfloor(xs);
        long yCell = Mth.lfloor(ys);
        long zCell = Mth.lfloor(zs);
        double xFrac = xs - xCell;
        double yFrac = ys - yCell;
        double zFrac = zs - zCell;
        return lattice.correction() * sampleAndLerp(lattice.permutations(), xCell, yCell, zCell, xFrac,
                yFrac - yFracFudge(yFrac, yScale, yFudge), zFrac, yFrac, lattice.x().period(), lattice.y().period(),
                lattice.z().period()) + lattice.anchor();
    }

    private static double sampleSheared(PeriodicLattice lattice, PeriodicLattice.Shear shear, double x, double y,
            double z, double yScale, double yFudge) {
        double worldX = slotValue(shear.xSlot(), x, y, z);
        double worldZ = slotValue(shear.zSlot(), x, y, z);
        double noiseX = shear.noiseX(worldX, worldZ);
        double noiseZ = shear.noiseZ(worldZ);
        double xs = shearedCoord(X_SLOT, shear, lattice.x(), x, noiseX, noiseZ) + lattice.xOffset();
        double ys = shearedCoord(Y_SLOT, shear, lattice.y(), y, noiseX, noiseZ) + lattice.yOffset();
        double zs = shearedCoord(Z_SLOT, shear, lattice.z(), z, noiseX, noiseZ) + lattice.zOffset();
        long cellX = Mth.lfloor(xs);
        long cellY = Mth.lfloor(ys);
        long cellZ = Mth.lfloor(zs);
        double xFrac = xs - cellX;
        double yFrac = ys - cellY;
        double zFrac = zs - cellZ;
        double yFracFudged = yFrac - yFracFudge(yFrac, yScale, yFudge);
        byte[] permutations = lattice.permutations();
        int xSlot = shear.xSlot();
        int zSlot = shear.zSlot();
        int freeSlot = X_SLOT + Y_SLOT + Z_SLOT - xSlot - zSlot;
        long xPairCell = slotCell(xSlot, cellX, cellY, cellZ);
        long zPairCell = slotCell(zSlot, cellX, cellY, cellZ);
        int pair00 = reducedPair(shear, xPairCell, zPairCell);
        int pair10 = reducedPair(shear, xPairCell + 1L, zPairCell);
        int pair01 = reducedPair(shear, xPairCell, zPairCell + 1L);
        int pair11 = reducedPair(shear, xPairCell + 1L, zPairCell + 1L);
        long freeCell = slotCell(freeSlot, cellX, cellY, cellZ);
        long freePeriod = getAxis(freeSlot, lattice).period();
        long free0 = wrapCell(freeCell, freePeriod);
        long free1 = wrapCell(freeCell + 1L, freePeriod);
        double d000 = gradDot(cornerHash(permutations, xSlot, zSlot, pair00, pair10, pair01, pair11, free0, free1,
                0, 0, 0), xFrac, yFracFudged, zFrac);
        double d100 = gradDot(cornerHash(permutations, xSlot, zSlot, pair00, pair10, pair01, pair11, free0, free1,
                1, 0, 0), xFrac - 1.0, yFracFudged, zFrac);
        double d010 = gradDot(cornerHash(permutations, xSlot, zSlot, pair00, pair10, pair01, pair11, free0, free1,
                0, 1, 0), xFrac, yFracFudged - 1.0, zFrac);
        double d110 = gradDot(cornerHash(permutations, xSlot, zSlot, pair00, pair10, pair01, pair11, free0, free1,
                1, 1, 0), xFrac - 1.0, yFracFudged - 1.0, zFrac);
        double d001 = gradDot(cornerHash(permutations, xSlot, zSlot, pair00, pair10, pair01, pair11, free0, free1,
                0, 0, 1), xFrac, yFracFudged, zFrac - 1.0);
        double d101 = gradDot(cornerHash(permutations, xSlot, zSlot, pair00, pair10, pair01, pair11, free0, free1,
                1, 0, 1), xFrac - 1.0, yFracFudged, zFrac - 1.0);
        double d011 = gradDot(cornerHash(permutations, xSlot, zSlot, pair00, pair10, pair01, pair11, free0, free1,
                0, 1, 1), xFrac, yFracFudged - 1.0, zFrac - 1.0);
        double d111 = gradDot(cornerHash(permutations, xSlot, zSlot, pair00, pair10, pair01, pair11, free0, free1,
                1, 1, 1), xFrac - 1.0, yFracFudged - 1.0, zFrac - 1.0);
        double noise = Mth.lerp3(Mth.smoothstep(xFrac), Mth.smoothstep(yFrac), Mth.smoothstep(zFrac),
                d000, d100, d010, d110, d001, d101, d011, d111);
        return lattice.correction() * noise + lattice.anchor();
    }

    private static double yFracFudge(double yFrac, double yScale, double yFudge) {
        if (yScale == 0.0) {
            return 0.0;
        }

        double fudgeLimit = yFudge >= 0.0 && yFudge < yFrac ? yFudge : yFrac;
        return Mth.floor(fudgeLimit / yScale + 1.0E-7F) * yScale;
    }

    private static double slotValue(int slot, double x, double y, double z) {
        return switch (slot) {
            case X_SLOT -> x;
            case Y_SLOT -> y;
            default -> z;
        };
    }

    private static double shearedCoord(int slot, PeriodicLattice.Shear shear, PeriodicLattice.Axis axis,
            double input, double noiseX, double noiseZ) {
        if (slot == shear.xSlot()) {
            return noiseX;
        }

        return slot == shear.zSlot() ? noiseZ : axis.coord(input);
    }

    private static int reducedPair(PeriodicLattice.Shear shear, long cellX, long cellZ) {
        long laps = Math.floorDiv(cellZ, shear.zPeriod());
        long reducedZ = cellZ - laps * shear.zPeriod();
        long reducedX = Math.floorMod(cellX - laps * shear.skewCells(), shear.xPeriod());
        return (int) (reducedX & PERMUTATION_MASK) | (int) (reducedZ & PERMUTATION_MASK) << PAIR_SHIFT;
    }

    private static int cornerHash(byte[] permutations, int xSlot, int zSlot, int pair00, int pair10, int pair01,
            int pair11, long free0, long free1, int dx, int dy, int dz) {
        int xStep = slotStep(xSlot, dx, dy, dz);
        int zStep = slotStep(zSlot, dx, dy, dz);
        int pair = xStep == 0 ? (zStep == 0 ? pair00 : pair01) : (zStep == 0 ? pair10 : pair11);
        long free = slotStep(X_SLOT + Y_SLOT + Z_SLOT - xSlot - zSlot, dx, dy, dz) == 0 ? free0 : free1;
        long x = pairedCell(X_SLOT, xSlot, zSlot, pair, free);
        long y = pairedCell(Y_SLOT, xSlot, zSlot, pair, free);
        long z = pairedCell(Z_SLOT, xSlot, zSlot, pair, free);
        return p(permutations, p(permutations, p(permutations, x) + y) + z);
    }

    private static int slotStep(int slot, int dx, int dy, int dz) {
        return switch (slot) {
            case X_SLOT -> dx;
            case Y_SLOT -> dy;
            default -> dz;
        };
    }

    private static long pairedCell(int slot, int xSlot, int zSlot, int pair, long free) {
        if (slot == xSlot) {
            return pair & PERMUTATION_MASK;
        }

        return slot == zSlot ? pair >>> PAIR_SHIFT : free;
    }

    private static PeriodicLattice.Axis getAxis(int slot, PeriodicLattice lattice) {
        return switch (slot) {
            case X_SLOT -> lattice.x();
            case Y_SLOT -> lattice.y();
            default -> lattice.z();
        };
    }

    private static long slotCell(int slot, long cellX, long cellY, long cellZ) {
        return switch (slot) {
            case X_SLOT -> cellX;
            case Y_SLOT -> cellY;
            default -> cellZ;
        };
    }

    public static double sampleLattice(byte[] permutations, double xs, double ys, double zs, long xPeriod,
            long zPeriod) {
        return sampleLattice(permutations, xs, ys, zs, xPeriod, UNBOUNDED_PERIOD, zPeriod);
    }

    public static double sampleLattice(byte[] permutations, double xs, double ys, double zs, long xPeriod,
            long yPeriod, long zPeriod) {
        long xCell = Mth.lfloor(xs);
        long yCell = Mth.lfloor(ys);
        long zCell = Mth.lfloor(zs);
        double xFrac = xs - xCell;
        double yFrac = ys - yCell;
        double zFrac = zs - zCell;
        return sampleAndLerp(permutations, xCell, yCell, zCell, xFrac, yFrac, zFrac, yFrac,
                xPeriod, yPeriod, zPeriod);
    }

    private static double anchorSample(byte[] permutations, WrapDomain xDomain, WrapDomain zDomain,
            long xPeriod, long zPeriod, double scale, double xOffset, double yOffset, double zOffset) {
        double xs = foldAndScale(xDomain, xPeriod, scale, 0.0) + xOffset;
        double zs = foldAndScale(zDomain, zPeriod, scale, 0.0) + zOffset;
        long xCell = Mth.lfloor(xs);
        long zCell = Mth.lfloor(zs);
        long yCell = Mth.lfloor(yOffset);
        double yFrac = yOffset - yCell;
        return sampleAndLerp(permutations, xCell, yCell, zCell, xs - xCell, yFrac, zs - zCell, yFrac,
                xPeriod, UNBOUNDED_PERIOD, zPeriod);
    }

    public static long period(WrapDomain domain, double scale, LapFloor floor) {
        if (!domain.isLooped()) {
            return UNBOUNDED_PERIOD;
        }

        long rounded = Math.round(domain.domainLength * scale);
        return rounded < 2L ? floor.period : rounded;
    }

    static double ratio(WrapDomain domain, long period) {
        return isClosed(period) ? (double) period / domain.domainLength : 0.0;
    }

    public static double foldAndScale(WrapDomain domain, long period, double scale, double coord) {
        return foldAndScale(domain, period, scale, ratio(domain, period), coord);
    }

    public static double foldAndScale(WrapDomain domain, long period, double scale, double ratio, double coord) {
        return period == UNBOUNDED_PERIOD
                ? PerlinNoise.wrap(coord * scale)
                : folded(domain, period, ratio, coord);
    }

    static double foldAndScaleSimplex(WrapDomain domain, long period, double scale, double ratio, double coord) {
        return period == UNBOUNDED_PERIOD ? coord * scale : folded(domain, period, ratio, coord);
    }

    private static double folded(WrapDomain domain, long period, double ratio, double coord) {
        return period == HELD_PERIOD ? 0.0 : domain.wrap(coord) * ratio;
    }

    private static double sampleAndLerp(byte[] permutations, long xCell, long yCell, long zCell,
            double xFrac, double yFracFudged, double zFrac, double yFracOriginal,
            long xPeriod, long yPeriod, long zPeriod) {
        long xWrapped = wrapCell(xCell, xPeriod);
        int x0 = p(permutations, xWrapped);
        int x1 = p(permutations, nextCell(xWrapped, xPeriod));
        long y0 = wrapCell(yCell, yPeriod);
        long y1 = nextCell(y0, yPeriod);
        int xy00 = p(permutations, x0 + y0);
        int xy01 = p(permutations, x0 + y1);
        int xy10 = p(permutations, x1 + y0);
        int xy11 = p(permutations, x1 + y1);
        long z0 = wrapCell(zCell, zPeriod);
        long z1 = nextCell(z0, zPeriod);
        double d000 = gradDot(p(permutations, xy00 + z0), xFrac, yFracFudged, zFrac);
        double d100 = gradDot(p(permutations, xy10 + z0), xFrac - 1.0, yFracFudged, zFrac);
        double d010 = gradDot(p(permutations, xy01 + z0), xFrac, yFracFudged - 1.0, zFrac);
        double d110 = gradDot(p(permutations, xy11 + z0), xFrac - 1.0, yFracFudged - 1.0, zFrac);
        double d001 = gradDot(p(permutations, xy00 + z1), xFrac, yFracFudged, zFrac - 1.0);
        double d101 = gradDot(p(permutations, xy10 + z1), xFrac - 1.0, yFracFudged, zFrac - 1.0);
        double d011 = gradDot(p(permutations, xy01 + z1), xFrac, yFracFudged - 1.0, zFrac - 1.0);
        double d111 = gradDot(p(permutations, xy11 + z1), xFrac - 1.0, yFracFudged - 1.0, zFrac - 1.0);
        double xAlpha = Mth.smoothstep(xFrac);
        double yAlpha = Mth.smoothstep(yFracOriginal);
        double zAlpha = Mth.smoothstep(zFrac);
        return Mth.lerp3(xAlpha, yAlpha, zAlpha, d000, d100, d010, d110, d001, d101, d011, d111);
    }

    static boolean isClosed(long period) {
        return period > UNBOUNDED_PERIOD;
    }

    private static long wrapCell(long cell, long period) {
        return isClosed(period) ? Math.floorMod(cell, period) : cell;
    }

    private static long nextCell(long wrapped, long period) {
        long next = wrapped + 1L;
        return isClosed(period) && next == period ? 0L : next;
    }

    private static int p(byte[] permutations, long index) {
        return permutations[(int) (index & 0xFFL)] & 0xFF;
    }

    private static double gradDot(int hash, double x, double y, double z) {
        int[] gradient = GRADIENT[hash & 15];
        return gradient[0] * x + gradient[1] * y + gradient[2] * z;
    }

    private PeriodicNoiseSampler() {
    }
}
