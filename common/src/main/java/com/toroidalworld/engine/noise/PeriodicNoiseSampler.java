package com.toroidalworld.engine.noise;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WrapDomain;

import net.minecraft.core.Direction;
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

    static final long UNBOUNDED_PERIOD = 0L;

    static final long HELD_PERIOD = -1L;

    static final double NO_FUDGE = 0.0;

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
        if (!frame.isDefault()) {
            SlotAxes axes = frame.axes();
            return new PeriodicLattice(permutations, xOffset, yOffset, zOffset, fudgeYScale,
                    slotAxis(axes.x(), transformer, frame, scale, floor),
                    slotAxis(axes.y(), transformer, frame, scale, floor),
                    slotAxis(axes.z(), transformer, frame, scale, floor), 1.0, 0.0);
        }

        WrapDomain xDomain = transformer.blockDomain(Direction.Axis.X);
        WrapDomain zDomain = transformer.blockDomain(Direction.Axis.Z);
        long xPeriod = period(xDomain, scale, floor);
        long zPeriod = period(zDomain, scale, floor);
        double verticalShare = frame.verticalShare();
        double correction = OctaveVarianceCorrection.factor(xDomain, zDomain, scale, verticalShare);
        double anchorGain = OctaveVarianceCorrection.anchorGain(xDomain, zDomain, scale, verticalShare);
        double anchor = anchorGain > 0.0
                ? anchorGain * anchorSample(permutations, xDomain, zDomain, xPeriod, zPeriod, scale, xOffset, yOffset,
                        zOffset)
                : 0.0;
        return new PeriodicLattice(permutations, xOffset, yOffset, zOffset, fudgeYScale,
                new PeriodicLattice.Axis(true, xDomain, xPeriod, scale), PeriodicLattice.Axis.PASS_THROUGH,
                new PeriodicLattice.Axis(true, zDomain, zPeriod, scale), correction, anchor);
    }

    private static PeriodicLattice.Axis slotAxis(SlotAxis axis, WorldFold transformer, NoiseFrame frame, double scale,
            LapFloor floor) {
        WrapDomain domain = axis.domainOf(transformer);
        double slotScale = scale / axis.divisorIn(frame);
        return new PeriodicLattice.Axis(axis.carriesWorldAxis(), domain, period(domain, slotScale, floor), slotScale);
    }

    static float sample(PeriodicLattice lattice, double x, double y, double z, double originalY) {
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
