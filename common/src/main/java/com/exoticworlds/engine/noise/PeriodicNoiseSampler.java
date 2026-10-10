package com.exoticworlds.engine.noise;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WrapDomain;

import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.synth.GradientNoise;

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

    private static final int LATTICE_OFFSET_SPAN = 256;

    static final long MAX_INDEXED_PERIOD = Integer.MAX_VALUE - LATTICE_OFFSET_SPAN;

    static final double NO_FUDGE = 0.0;

    private static final int X_SLOT = 0;
    private static final int Y_SLOT = 1;
    private static final int Z_SLOT = 2;
    private static final int NO_SLOT = -1;

    private static final float FUDGE_EPSILON = 1.0E-7F;

    public static float sample(byte[] permutations, double xOffset, double yOffset, double zOffset,
            WorldFold transformer, NoiseFrame frame, double scale, double x, double y, double z) {
        return sample(permutations, xOffset, yOffset, zOffset, transformer, frame, scale, x, y, z,
                LapFloor.of(transformer));
    }

    public static float sampleSmeared(byte[] permutations, double xOffset, double yOffset, double zOffset,
            WorldFold transformer, NoiseFrame frame, double scale, double x, double y, double z,
            double originalY, double fudgeYScale) {
        return sample(lattice(permutations, xOffset, yOffset, zOffset, fudgeYScale, transformer, frame, scale,
                LapFloor.of(transformer)), x, y, z, originalY);
    }

    public static float sample(byte[] permutations, double xOffset, double yOffset, double zOffset,
            WorldFold transformer, NoiseFrame frame, double scale, double x, double y, double z, LapFloor floor) {
        return sample(lattice(permutations, xOffset, yOffset, zOffset, NO_FUDGE, transformer, frame, scale, floor),
                x, y, z, y);
    }

    static PeriodicLattice lattice(byte[] permutations, double xOffset, double yOffset, double zOffset,
            double fudgeYScale, WorldFold transformer, NoiseFrame frame, double scale, LapFloor floor) {
        TranslationLattice translations = transformer.blockLattice();
        if (!frame.isDefault()) {
            SlotAxes axes = frame.axes();
            PeriodicLattice.Axis xAxis = slotAxis(axes.x(), translations, frame, scale, floor);
            PeriodicLattice.Axis yAxis = slotAxis(axes.y(), translations, frame, scale, floor);
            PeriodicLattice.Axis zAxis = slotAxis(axes.z(), translations, frame, scale, floor);
            return new PeriodicLattice(permutations, xOffset, yOffset, zOffset, fudgeYScale, xAxis, yAxis, zAxis,
                    1.0, 0.0, slotShear(translations, axes, xAxis, yAxis, zAxis));
        }

        WrapDomain xDomain = translations.x();
        WrapDomain zDomain = translations.z();
        long xPeriod = period(xDomain, scale, floor);
        long zPeriod = period(zDomain, scale, floor);
        double verticalShare = frame.verticalShare();
        double correction = OctaveVarianceCorrection.factor(xDomain, zDomain, scale, verticalShare);
        double anchorGain = OctaveVarianceCorrection.anchorGain(xDomain, zDomain, scale, verticalShare);
        PeriodicLattice.Axis xAxis = new PeriodicLattice.Axis(true, xDomain, xPeriod, scale);
        PeriodicLattice.Axis zAxis = new PeriodicLattice.Axis(true, zDomain, zPeriod, scale);
        if (translations.isSkewed()) {
            PeriodicLattice.Shear shear = PeriodicLattice.Shear.of(translations, X_SLOT, Z_SLOT, xPeriod, zPeriod);
            double anchor = anchorGain > 0.0
                    ? anchorGain * sample(new PeriodicLattice(permutations, xOffset, yOffset, zOffset, NO_FUDGE,
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
        return new PeriodicLattice.Axis(axis.carriesWorldAxis(), domain, period(domain, slotScale, floor), slotScale);
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
        int xCell = Mth.floor(xs);
        int yCell = Mth.floor(ys);
        int zCell = Mth.floor(zs);
        float yFrac = (float) (ys - yCell);
        return sampleAndLerp(permutations, xCell, yCell, zCell, (float) (xs - xCell), yFrac, (float) (zs - zCell),
                yFrac, xPeriod, UNBOUNDED_PERIOD, zPeriod);
    }

    static float sample(PeriodicLattice lattice, double x, double y, double z, double originalY) {
        PeriodicLattice.Shear shear = lattice.shear();
        if (shear != null) {
            return sampleSheared(lattice, shear, x, y, z, originalY);
        }

        double xs = lattice.x().coord(x) + lattice.xOffset();
        double ys = lattice.y().coord(y) + lattice.yOffset();
        double zs = lattice.z().coord(z) + lattice.zOffset();
        int xCell = Mth.floor(xs);
        int yCell = Mth.floor(ys);
        int zCell = Mth.floor(zs);
        float xFrac = (float) (xs - xCell);
        double yRelative = ys - yCell;
        float zFrac = (float) (zs - zCell);
        double fudgeYScale = lattice.fudgeYScale();
        float yFracFudged = fudgeYScale == NO_FUDGE
                ? (float) yRelative
                : (float) (yRelative - fudgeY(originalY, yRelative, fudgeYScale));
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
        int xCell = Mth.floor(xs);
        int yCell = Mth.floor(ys);
        int zCell = Mth.floor(zs);
        float xFrac = (float) (xs - xCell);
        double yRelative = ys - yCell;
        float zFrac = (float) (zs - zCell);
        double fudgeYScale = lattice.fudgeYScale();
        float yFracFudged = fudgeYScale == NO_FUDGE
                ? (float) yRelative
                : (float) (yRelative - fudgeY(originalY, yRelative, fudgeYScale));
        byte[] permutations = lattice.permutations();
        float d000 = gradDot(shearedHash(permutations, lattice, shear, xCell, yCell, zCell),
                xFrac, yFracFudged, zFrac);
        float d100 = gradDot(shearedHash(permutations, lattice, shear, xCell + 1L, yCell, zCell),
                xFrac - 1.0F, yFracFudged, zFrac);
        float d010 = gradDot(shearedHash(permutations, lattice, shear, xCell, yCell + 1L, zCell),
                xFrac, yFracFudged - 1.0F, zFrac);
        float d110 = gradDot(shearedHash(permutations, lattice, shear, xCell + 1L, yCell + 1L, zCell),
                xFrac - 1.0F, yFracFudged - 1.0F, zFrac);
        float d001 = gradDot(shearedHash(permutations, lattice, shear, xCell, yCell, zCell + 1L),
                xFrac, yFracFudged, zFrac - 1.0F);
        float d101 = gradDot(shearedHash(permutations, lattice, shear, xCell + 1L, yCell, zCell + 1L),
                xFrac - 1.0F, yFracFudged, zFrac - 1.0F);
        float d011 = gradDot(shearedHash(permutations, lattice, shear, xCell, yCell + 1L, zCell + 1L),
                xFrac, yFracFudged - 1.0F, zFrac - 1.0F);
        float d111 = gradDot(shearedHash(permutations, lattice, shear, xCell + 1L, yCell + 1L, zCell + 1L),
                xFrac - 1.0F, yFracFudged - 1.0F, zFrac - 1.0F);
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

    private static int shearedHash(byte[] permutations, PeriodicLattice lattice, PeriodicLattice.Shear shear,
            long xCell, long yCell, long zCell) {
        long worldZ = slotCell(shear.zSlot(), xCell, yCell, zCell);
        long laps = Math.floorDiv(worldZ, shear.zPeriod());
        long reducedZ = worldZ - laps * shear.zPeriod();
        long reducedX = Math.floorMod(slotCell(shear.xSlot(), xCell, yCell, zCell) - laps * shear.skewCells(),
                shear.xPeriod());
        long x = reducedCell(X_SLOT, shear, lattice.x(), xCell, reducedX, reducedZ);
        long y = reducedCell(Y_SLOT, shear, lattice.y(), yCell, reducedX, reducedZ);
        long z = reducedCell(Z_SLOT, shear, lattice.z(), zCell, reducedX, reducedZ);
        return p(permutations, p(permutations, p(permutations, x) + y) + z);
    }

    private static long slotCell(int slot, long xCell, long yCell, long zCell) {
        return switch (slot) {
            case X_SLOT -> xCell;
            case Y_SLOT -> yCell;
            default -> zCell;
        };
    }

    private static long reducedCell(int slot, PeriodicLattice.Shear shear, PeriodicLattice.Axis axis, long cell,
            long reducedX, long reducedZ) {
        if (slot == shear.xSlot()) {
            return reducedX;
        }

        return slot == shear.zSlot() ? reducedZ : wrapCell(cell, axis.period());
    }

    private static double fudgeY(double originalY, double yRelative, double fudgeYScale) {
        double fudgeLimit = originalY >= 0.0 && originalY < yRelative ? originalY : yRelative;
        return Mth.floor(fudgeLimit / fudgeYScale + FUDGE_EPSILON) * fudgeYScale;
    }

    private static float anchorSample(byte[] permutations, WrapDomain xDomain, WrapDomain zDomain,
            long xPeriod, long zPeriod, double scale, double xOffset, double yOffset, double zOffset) {
        double xs = foldAndScale(xDomain, xPeriod, scale, 0.0) + xOffset;
        double zs = foldAndScale(zDomain, zPeriod, scale, 0.0) + zOffset;
        int xCell = Mth.floor(xs);
        int zCell = Mth.floor(zs);
        int yCell = Mth.floor(yOffset);
        float yFrac = (float) (yOffset - yCell);
        return sampleAndLerp(permutations, xCell, yCell, zCell, (float) (xs - xCell), yFrac, (float) (zs - zCell),
                yFrac, xPeriod, UNBOUNDED_PERIOD, zPeriod);
    }

    static long period(WrapDomain domain, double scale, LapFloor floor) {
        if (!domain.loops()) {
            return UNBOUNDED_PERIOD;
        }

        long rounded = Math.round(domain.domainLength * scale);
        return rounded < 2L ? floor.period : rounded;
    }

    static double foldAndScale(WrapDomain domain, long period, double scale, double coord) {
        return period == UNBOUNDED_PERIOD
                ? GradientNoise.wrap(coord * scale)
                : folded(domain, period, coord);
    }

    static double foldAndScaleSimplex(WrapDomain domain, long period, double scale, double coord) {
        return period == UNBOUNDED_PERIOD ? coord * scale : folded(domain, period, coord);
    }

    private static double folded(WrapDomain domain, long period, double coord) {
        return period == HELD_PERIOD ? 0.0 : domain.wrap(coord) * ((double) period / domain.domainLength);
    }

    private static float sampleAndLerp(byte[] permutations, int xCell, int yCell, int zCell,
            float xFrac, float yFracFudged, float zFrac, float yFracOriginal,
            long xPeriod, long yPeriod, long zPeriod) {
        int x0 = p(permutations, wrapCell(xCell, xPeriod));
        int x1 = p(permutations, wrapCell(xCell + 1L, xPeriod));
        long y0 = wrapCell(yCell, yPeriod);
        long y1 = wrapCell(yCell + 1L, yPeriod);
        int xy00 = p(permutations, x0 + y0);
        int xy01 = p(permutations, x0 + y1);
        int xy10 = p(permutations, x1 + y0);
        int xy11 = p(permutations, x1 + y1);
        long z0 = wrapCell(zCell, zPeriod);
        long z1 = wrapCell(zCell + 1L, zPeriod);
        float d000 = gradDot(p(permutations, xy00 + z0), xFrac, yFracFudged, zFrac);
        float d100 = gradDot(p(permutations, xy10 + z0), xFrac - 1.0F, yFracFudged, zFrac);
        float d010 = gradDot(p(permutations, xy01 + z0), xFrac, yFracFudged - 1.0F, zFrac);
        float d110 = gradDot(p(permutations, xy11 + z0), xFrac - 1.0F, yFracFudged - 1.0F, zFrac);
        float d001 = gradDot(p(permutations, xy00 + z1), xFrac, yFracFudged, zFrac - 1.0F);
        float d101 = gradDot(p(permutations, xy10 + z1), xFrac - 1.0F, yFracFudged, zFrac - 1.0F);
        float d011 = gradDot(p(permutations, xy01 + z1), xFrac, yFracFudged - 1.0F, zFrac - 1.0F);
        float d111 = gradDot(p(permutations, xy11 + z1), xFrac - 1.0F, yFracFudged - 1.0F, zFrac - 1.0F);
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

    private static int p(byte[] permutations, long index) {
        return permutations[(int) (index & 0xFFL)] & 0xFF;
    }

    private static float gradDot(int hash, float x, float y, float z) {
        int[] gradient = GRADIENT[hash & 15];
        return gradient[0] * x + gradient[1] * y + gradient[2] * z;
    }

    private PeriodicNoiseSampler() {
    }
}
