package com.exoticworlds.engine.noise.periodic;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WrapDomain;
import com.exoticworlds.engine.noise.NoiseFrame;
import com.exoticworlds.engine.noise.PerlinMath;
import com.exoticworlds.engine.noise.SlotAxes;
import com.exoticworlds.engine.noise.SlotAxis;

import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.synth.GradientNoise;

public final class PeriodicNoiseSampler {
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
            double fudgeYScale, WorldFold fold, NoiseFrame frame, double scale, LapFloor floor) {
        TranslationLattice translations = fold.blockLattice();
        return frame.isDefault()
                ? worldLattice(permutations, xOffset, yOffset, zOffset, fudgeYScale, translations, frame, scale,
                        floor)
                : slottedLattice(permutations, xOffset, yOffset, zOffset, fudgeYScale, translations, frame, scale,
                        floor);
    }

    private static PeriodicLattice slottedLattice(byte[] permutations, double xOffset, double yOffset,
            double zOffset, double fudgeYScale, TranslationLattice translations, NoiseFrame frame, double scale,
            LapFloor floor) {
        SlotAxes axes = frame.axes();
        PeriodicLattice.Axis xAxis = slotAxis(axes.x(), translations, frame, scale, floor);
        PeriodicLattice.Axis yAxis = slotAxis(axes.y(), translations, frame, scale, floor);
        PeriodicLattice.Axis zAxis = slotAxis(axes.z(), translations, frame, scale, floor);
        return new PeriodicLattice(permutations, xOffset, yOffset, zOffset, fudgeYScale, xAxis, yAxis, zAxis,
                1.0, 0.0, slotShear(translations, axes, xAxis, yAxis, zAxis));
    }

    private static PeriodicLattice worldLattice(byte[] permutations, double xOffset, double yOffset,
            double zOffset, double fudgeYScale, TranslationLattice translations, NoiseFrame frame, double scale,
            LapFloor floor) {
        WrapDomain xDomain = translations.x();
        WrapDomain zDomain = translations.z();
        long xPeriod = period(xDomain, scale, floor);
        long zPeriod = period(zDomain, scale, floor);
        double verticalShare = frame.verticalShare();
        double correction = OctaveVarianceCorrection.factor(xDomain, zDomain, scale, verticalShare);
        double anchorGain = OctaveVarianceCorrection.anchorGain(xDomain, zDomain, scale, verticalShare);
        PeriodicLattice.Axis xAxis = PeriodicLattice.Axis.of(true, xDomain, xPeriod, scale);
        PeriodicLattice.Axis zAxis = PeriodicLattice.Axis.of(true, zDomain, zPeriod, scale);
        if (translations.isSkewed()) {
            PeriodicLattice.Shear shear = PeriodicLattice.Shear.of(translations, X_SLOT, Z_SLOT, xPeriod, zPeriod);
            double anchor = anchorGain > 0.0
                    ? anchorGain * sample(new PeriodicLattice(permutations, xOffset, yOffset, zOffset, PerlinMath.NO_FUDGE,
                            xAxis, PeriodicLattice.Axis.PASS_THROUGH, zAxis, 1.0, 0.0, shear), 0.0, 0.0, 0.0, 0.0)
                    : 0.0;
            return new PeriodicLattice(permutations, xOffset, yOffset, zOffset, fudgeYScale,
                    xAxis, PeriodicLattice.Axis.PASS_THROUGH, zAxis, correction, anchor, shear);
        }

        double anchor = anchorGain > 0.0
                ? anchorGain * anchorSample(permutations, xDomain, zDomain, xPeriod, zPeriod, scale, xOffset, yOffset,
                        zOffset)
                : 0.0;
        return new PeriodicLattice(permutations, xOffset, yOffset, zOffset, fudgeYScale,
                xAxis, PeriodicLattice.Axis.PASS_THROUGH, zAxis, correction, anchor, null);
    }

    private static PeriodicLattice.Axis slotAxis(SlotAxis axis, TranslationLattice translations, NoiseFrame frame,
            double scale, LapFloor floor) {
        WrapDomain domain = axis.domainOf(translations);
        double slotScale = scale / axis.divisorIn(frame);
        return PeriodicLattice.Axis.of(axis.carriesWorldAxis(), domain, period(domain, slotScale, floor), slotScale);
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

    public static float sampleLattice(byte[] permutations, double xs, double ys, double zs, long xPeriod,
            long zPeriod) {
        long xCell = Mth.lfloor(xs);
        long yCell = Mth.lfloor(ys);
        long zCell = Mth.lfloor(zs);
        float yFrac = (float) (ys - yCell);
        return sampleAndLerp(permutations, xCell, yCell, zCell, (float) (xs - xCell), yFrac, (float) (zs - zCell),
                yFrac, xPeriod, UNBOUNDED_PERIOD, zPeriod);
    }

    public static float sample(PeriodicLattice lattice, double x, double y, double z, double originalY) {
        PeriodicLattice.Shear shear = lattice.shear();
        if (shear != null) {
            return sampleSheared(lattice, shear, x, y, z, originalY);
        }

        double xs = lattice.x().coord(x) + lattice.xOffset();
        double ys = lattice.y().coord(y) + lattice.yOffset();
        double zs = lattice.z().coord(z) + lattice.zOffset();
        long xCell = Mth.lfloor(xs);
        long yCell = Mth.lfloor(ys);
        long zCell = Mth.lfloor(zs);
        float xFrac = (float) (xs - xCell);
        double yRelative = ys - yCell;
        float zFrac = (float) (zs - zCell);
        double fudgeYScale = lattice.fudgeYScale();
        float yFracFudged = fudgeYScale == PerlinMath.NO_FUDGE
                ? (float) yRelative
                : (float) (yRelative - PerlinMath.fudgeY(originalY, yRelative, fudgeYScale));
        float noise = sampleAndLerp(lattice.permutations(), xCell, yCell, zCell, xFrac, yFracFudged, zFrac,
                (float) yRelative, lattice.x().period(), lattice.y().period(), lattice.z().period());
        return (float) (lattice.correction() * noise) + (float) lattice.anchor();
    }

    private static float sampleSheared(PeriodicLattice lattice, PeriodicLattice.Shear shear, double x, double y,
            double z, double originalY) {
        double worldX = slotValue(shear.xSlot(), x, y, z);
        double worldZ = slotValue(shear.zSlot(), x, y, z);
        double noiseX = shear.noiseX(worldX, worldZ);
        double noiseZ = shear.noiseZ(worldZ);
        double xs = shearedCoord(X_SLOT, shear, lattice.x(), x, noiseX, noiseZ) + lattice.xOffset();
        double ys = shearedCoord(Y_SLOT, shear, lattice.y(), y, noiseX, noiseZ) + lattice.yOffset();
        double zs = shearedCoord(Z_SLOT, shear, lattice.z(), z, noiseX, noiseZ) + lattice.zOffset();
        long xCell = Mth.lfloor(xs);
        long yCell = Mth.lfloor(ys);
        long zCell = Mth.lfloor(zs);
        float xFrac = (float) (xs - xCell);
        double yRelative = ys - yCell;
        float zFrac = (float) (zs - zCell);
        double fudgeYScale = lattice.fudgeYScale();
        float yFracFudged = fudgeYScale == PerlinMath.NO_FUDGE
                ? (float) yRelative
                : (float) (yRelative - PerlinMath.fudgeY(originalY, yRelative, fudgeYScale));
        byte[] permutations = lattice.permutations();
        int xSlot = shear.xSlot();
        int zSlot = shear.zSlot();
        int freeSlot = X_SLOT + Y_SLOT + Z_SLOT - xSlot - zSlot;
        long xPairCell = slotCell(xSlot, xCell, yCell, zCell);
        long zPairCell = slotCell(zSlot, xCell, yCell, zCell);
        int pair00 = reducedPair(shear, xPairCell, zPairCell);
        int pair10 = reducedPair(shear, xPairCell + 1L, zPairCell);
        int pair01 = reducedPair(shear, xPairCell, zPairCell + 1L);
        int pair11 = reducedPair(shear, xPairCell + 1L, zPairCell + 1L);
        long freeCell = slotCell(freeSlot, xCell, yCell, zCell);
        long freePeriod = axisAt(freeSlot, lattice).period();
        long free0 = wrapCell(freeCell, freePeriod);
        long free1 = wrapCell(freeCell + 1L, freePeriod);
        float d000 = gradDot(cornerHash(permutations, xSlot, zSlot, pair00, pair10, pair01, pair11, free0, free1,
                0, 0, 0), xFrac, yFracFudged, zFrac);
        float d100 = gradDot(cornerHash(permutations, xSlot, zSlot, pair00, pair10, pair01, pair11, free0, free1,
                1, 0, 0), xFrac - 1.0F, yFracFudged, zFrac);
        float d010 = gradDot(cornerHash(permutations, xSlot, zSlot, pair00, pair10, pair01, pair11, free0, free1,
                0, 1, 0), xFrac, yFracFudged - 1.0F, zFrac);
        float d110 = gradDot(cornerHash(permutations, xSlot, zSlot, pair00, pair10, pair01, pair11, free0, free1,
                1, 1, 0), xFrac - 1.0F, yFracFudged - 1.0F, zFrac);
        float d001 = gradDot(cornerHash(permutations, xSlot, zSlot, pair00, pair10, pair01, pair11, free0, free1,
                0, 0, 1), xFrac, yFracFudged, zFrac - 1.0F);
        float d101 = gradDot(cornerHash(permutations, xSlot, zSlot, pair00, pair10, pair01, pair11, free0, free1,
                1, 0, 1), xFrac - 1.0F, yFracFudged, zFrac - 1.0F);
        float d011 = gradDot(cornerHash(permutations, xSlot, zSlot, pair00, pair10, pair01, pair11, free0, free1,
                0, 1, 1), xFrac, yFracFudged - 1.0F, zFrac - 1.0F);
        float d111 = gradDot(cornerHash(permutations, xSlot, zSlot, pair00, pair10, pair01, pair11, free0, free1,
                1, 1, 1), xFrac - 1.0F, yFracFudged - 1.0F, zFrac - 1.0F);
        float noise = Mth.lerp3(Mth.smoothstep(xFrac), Mth.smoothstep((float) yRelative), Mth.smoothstep(zFrac),
                d000, d100, d010, d110, d001, d101, d011, d111);
        return (float) (lattice.correction() * noise) + (float) lattice.anchor();
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

    private static int reducedPair(PeriodicLattice.Shear shear, long xCell, long zCell) {
        long laps = Math.floorDiv(zCell, shear.zPeriod());
        long reducedZ = zCell - laps * shear.zPeriod();
        long reducedX = Math.floorMod(xCell - laps * shear.skewCells(), shear.xPeriod());
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
        return PerlinMath.p(permutations, PerlinMath.p(permutations, PerlinMath.p(permutations, x) + y) + z);
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

    private static PeriodicLattice.Axis axisAt(int slot, PeriodicLattice lattice) {
        return switch (slot) {
            case X_SLOT -> lattice.x();
            case Y_SLOT -> lattice.y();
            default -> lattice.z();
        };
    }

    private static long slotCell(int slot, long xCell, long yCell, long zCell) {
        return switch (slot) {
            case X_SLOT -> xCell;
            case Y_SLOT -> yCell;
            default -> zCell;
        };
    }

    private static float anchorSample(byte[] permutations, WrapDomain xDomain, WrapDomain zDomain,
            long xPeriod, long zPeriod, double scale, double xOffset, double yOffset, double zOffset) {
        double xs = foldAndScale(xDomain, xPeriod, scale, 0.0) + xOffset;
        double zs = foldAndScale(zDomain, zPeriod, scale, 0.0) + zOffset;
        long xCell = Mth.lfloor(xs);
        long zCell = Mth.lfloor(zs);
        long yCell = Mth.lfloor(yOffset);
        float yFrac = (float) (yOffset - yCell);
        return sampleAndLerp(permutations, xCell, yCell, zCell, (float) (xs - xCell), yFrac, (float) (zs - zCell),
                yFrac, xPeriod, UNBOUNDED_PERIOD, zPeriod);
    }

    public static long period(WrapDomain domain, double scale, LapFloor floor) {
        if (!domain.loops()) {
            return UNBOUNDED_PERIOD;
        }

        long rounded = Math.round(domain.domainLength * scale);
        return rounded < 2L ? floor.period : rounded;
    }

    static double ratio(WrapDomain domain, long period) {
        return closes(period) ? (double) period / domain.domainLength : 0.0;
    }

    public static double foldAndScale(WrapDomain domain, long period, double scale, double coord) {
        return foldAndScale(domain, period, scale, ratio(domain, period), coord);
    }

    static double foldAndScale(WrapDomain domain, long period, double scale, double ratio, double coord) {
        return period == UNBOUNDED_PERIOD
                ? GradientNoise.wrap(coord * scale)
                : folded(domain, period, ratio, coord);
    }

    static double foldAndScaleSimplex(WrapDomain domain, long period, double scale, double ratio, double coord) {
        return period == UNBOUNDED_PERIOD ? coord * scale : folded(domain, period, ratio, coord);
    }

    private static double folded(WrapDomain domain, long period, double ratio, double coord) {
        return period == HELD_PERIOD ? 0.0 : domain.wrap(coord) * ratio;
    }

    private static float sampleAndLerp(byte[] permutations, long xCell, long yCell, long zCell,
            float xFrac, float yFracFudged, float zFrac, float yFracOriginal,
            long xPeriod, long yPeriod, long zPeriod) {
        long xWrapped = wrapCell(xCell, xPeriod);
        int x0 = PerlinMath.p(permutations, xWrapped);
        int x1 = PerlinMath.p(permutations, nextCell(xWrapped, xPeriod));
        long y0 = wrapCell(yCell, yPeriod);
        long y1 = nextCell(y0, yPeriod);
        int xy00 = PerlinMath.p(permutations, x0 + y0);
        int xy01 = PerlinMath.p(permutations, x0 + y1);
        int xy10 = PerlinMath.p(permutations, x1 + y0);
        int xy11 = PerlinMath.p(permutations, x1 + y1);
        long z0 = wrapCell(zCell, zPeriod);
        long z1 = nextCell(z0, zPeriod);
        float d000 = gradDot(PerlinMath.p(permutations, xy00 + z0), xFrac, yFracFudged, zFrac);
        float d100 = gradDot(PerlinMath.p(permutations, xy10 + z0), xFrac - 1.0F, yFracFudged, zFrac);
        float d010 = gradDot(PerlinMath.p(permutations, xy01 + z0), xFrac, yFracFudged - 1.0F, zFrac);
        float d110 = gradDot(PerlinMath.p(permutations, xy11 + z0), xFrac - 1.0F, yFracFudged - 1.0F, zFrac);
        float d001 = gradDot(PerlinMath.p(permutations, xy00 + z1), xFrac, yFracFudged, zFrac - 1.0F);
        float d101 = gradDot(PerlinMath.p(permutations, xy10 + z1), xFrac - 1.0F, yFracFudged, zFrac - 1.0F);
        float d011 = gradDot(PerlinMath.p(permutations, xy01 + z1), xFrac, yFracFudged - 1.0F, zFrac - 1.0F);
        float d111 = gradDot(PerlinMath.p(permutations, xy11 + z1), xFrac - 1.0F, yFracFudged - 1.0F, zFrac - 1.0F);
        float xAlpha = Mth.smoothstep(xFrac);
        float yAlpha = Mth.smoothstep(yFracOriginal);
        float zAlpha = Mth.smoothstep(zFrac);
        return Mth.lerp3(xAlpha, yAlpha, zAlpha, d000, d100, d010, d110, d001, d101, d011, d111);
    }

    static boolean closes(long period) {
        return period > UNBOUNDED_PERIOD;
    }

    private static long wrapCell(long cell, long period) {
        return closes(period) ? Math.floorMod(cell, period) : cell;
    }

    private static long nextCell(long wrapped, long period) {
        long next = wrapped + 1L;
        return closes(period) && next == period ? 0L : next;
    }

    private static float gradDot(int hash, float x, float y, float z) {
        int[] gradient = PerlinMath.gradient(hash);
        return gradient[0] * x + gradient[1] * y + gradient[2] * z;
    }

    private PeriodicNoiseSampler() {
    }
}
