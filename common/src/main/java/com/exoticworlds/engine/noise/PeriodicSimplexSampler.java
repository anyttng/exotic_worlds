package com.exoticworlds.engine.noise;

import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WrapDomain;

import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.synth.NoiseStack;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

public final class PeriodicSimplexSampler {
    private static final double SQRT_3 = Math.sqrt(3.0);
    static final double VANILLA_SKEW = 0.5 * (SQRT_3 - 1.0);
    private static final double VANILLA_UNSKEW = (3.0 - SQRT_3) / 6.0;

    private static final double KERNEL_RADIUS_SQR = 0.5;
    private static final double NORMALIZATION = 70.0;
    private static final int GRADIENT_COUNT = 12;
    private static final int PERMUTATION_MASK = 0xFF;

    public static float sampleStack(WorldFold transformer, double scale, NoiseStack stack, double x, double z) {
        float value = 0.0F;
        for (NoiseStack.Layer layer : stack.layers) {
            if (!(layer.noise() instanceof SimplexNoise leaf)) {
                throw new IllegalArgumentException("A periodic simplex stack samples simplex leaves only, got "
                        + layer.noise().getClass().getName());
            }

            value += layer.amplitude() * sample(leaf, transformer, scale * layer.frequency(), x, z);
        }

        return value;
    }

    public static float sample(SimplexNoise noise, WorldFold transformer, double scale, double x, double z) {
        return (float) sample(noise.perms, noise.offsetX, noise.offsetY, transformer, scale, x, z);
    }

    static double sample(byte[] permutations, double xOffset, double zOffset,
            WorldFold transformer, double scale, double x, double z) {
        TranslationLattice lattice = transformer.blockLattice();
        WrapDomain xDomain = lattice.x();
        WrapDomain zDomain = lattice.z();
        LapFloor floor = LapFloor.of(transformer);
        long xPeriod = PeriodicNoiseSampler.period(xDomain, scale, floor);
        long zPeriod = PeriodicNoiseSampler.period(zDomain, scale, floor);
        if (lattice.isSkewed()) {
            return sampleSkewed(permutations, xOffset, zOffset, lattice, xPeriod, zPeriod, x, z);
        }

        double xs = PeriodicNoiseSampler.foldAndScaleSimplex(xDomain, xPeriod, scale, x) + xOffset;
        double zs = PeriodicNoiseSampler.foldAndScaleSimplex(zDomain, zPeriod, scale, z) + zOffset;
        long xLattice = lattice(xPeriod);
        long zLattice = lattice(zPeriod);
        long denominator = skewDenominator(xLattice, zLattice);
        long numerator = skewNumerator(denominator);

        double skew;
        double unskew;
        if (denominator == PeriodicNoiseSampler.UNBOUNDED_PERIOD) {
            skew = VANILLA_SKEW;
            unskew = VANILLA_UNSKEW;
        } else {
            skew = (double) numerator / denominator;
            unskew = skew / (1.0 + 2.0 * skew);
        }

        long xLapV = denominator == PeriodicNoiseSampler.UNBOUNDED_PERIOD ? 0L : numerator * (xLattice / denominator);
        long zLapU = denominator == PeriodicNoiseSampler.UNBOUNDED_PERIOD ? 0L : numerator * (zLattice / denominator);
        long xLapU = xLattice + xLapV;
        long zLapV = zLattice + zLapU;
        return kernel(permutations, xs, zs, skew, unskew, xLapU, xLapV, zLapU, zLapV);
    }

    private static double sampleSkewed(byte[] permutations, double xOffset, double zOffset,
            TranslationLattice lattice, long xPeriod, long zPeriod, double x, double z) {
        double width = lattice.x().domainLength;
        double height = lattice.z().domainLength;
        double skewBlocks = lattice.skew();
        double sheared = xPeriod * skewBlocks / width;
        long xLapU = Math.round(xPeriod + VANILLA_SKEW * xPeriod);
        long xLapV = Math.round(VANILLA_SKEW * xPeriod);
        long zLapU = Math.round(sheared + VANILLA_SKEW * (sheared + zPeriod));
        long zLapV = Math.round(zPeriod + VANILLA_SKEW * (sheared + zPeriod));
        double xLapX = xLapU - VANILLA_UNSKEW * (xLapU + xLapV);
        double xLapZ = xLapV - VANILLA_UNSKEW * (xLapU + xLapV);
        double zLapX = zLapU - VANILLA_UNSKEW * (zLapU + zLapV);
        double zLapZ = zLapV - VANILLA_UNSKEW * (zLapU + zLapV);
        double foldedX = lattice.foldX(x, z);
        double foldedZ = lattice.foldZ(z);
        double xLaps = foldedX / width - foldedZ * skewBlocks / (width * height);
        double zLaps = foldedZ / height;
        double xs = xLaps * xLapX + zLaps * zLapX + xOffset;
        double zs = xLaps * xLapZ + zLaps * zLapZ + zOffset;
        return kernel(permutations, xs, zs, VANILLA_SKEW, VANILLA_UNSKEW, xLapU, xLapV, zLapU, zLapV);
    }

    private static double kernel(byte[] permutations, double xs, double zs, double skew, double unskew,
            long xLapU, long xLapV, long zLapU, long zLapV) {
        double skewed = (xs + zs) * skew;
        long uCell = Mth.lfloor(xs + skewed);
        long vCell = Mth.lfloor(zs + skewed);
        double unskewed = (uCell + vCell) * unskew;
        double xFrac = xs - (uCell - unskewed);
        double zFrac = zs - (vCell - unskewed);

        long uStep;
        long vStep;
        if (xFrac > zFrac) {
            uStep = 1L;
            vStep = 0L;
        } else {
            uStep = 0L;
            vStep = 1L;
        }

        double xMid = xFrac - uStep + unskew;
        double zMid = zFrac - vStep + unskew;
        double xFar = xFrac - 1.0 + 2.0 * unskew;
        double zFar = zFrac - 1.0 + 2.0 * unskew;

        double near = cornerNoise(
                gradient(permutations, uCell, vCell, xLapU, xLapV, zLapU, zLapV), xFrac, zFrac);
        double mid = cornerNoise(
                gradient(permutations, uCell + uStep, vCell + vStep, xLapU, xLapV, zLapU, zLapV), xMid, zMid);
        double far = cornerNoise(
                gradient(permutations, uCell + 1L, vCell + 1L, xLapU, xLapV, zLapU, zLapV), xFar, zFar);

        return NORMALIZATION * (near + mid + far);
    }

    static long skewDenominator(long xPeriod, long zPeriod) {
        if (xPeriod == PeriodicNoiseSampler.UNBOUNDED_PERIOD) {
            return zPeriod;
        }

        if (zPeriod == PeriodicNoiseSampler.UNBOUNDED_PERIOD) {
            return xPeriod;
        }

        return gcd(xPeriod, zPeriod);
    }

    static long skewNumerator(long denominator) {
        return denominator == PeriodicNoiseSampler.UNBOUNDED_PERIOD ? 0L : (long) Math.floor(VANILLA_SKEW * denominator);
    }

    private static long gcd(long first, long second) {
        long larger = first;
        long smaller = second;
        while (smaller != 0L) {
            long remainder = larger % smaller;
            larger = smaller;
            smaller = remainder;
        }

        return larger;
    }

    private static long lattice(long period) {
        return PeriodicNoiseSampler.closes(period) ? period : PeriodicNoiseSampler.UNBOUNDED_PERIOD;
    }

    private static int gradient(byte[] permutations, long u, long v,
            long xLapU, long xLapV, long zLapU, long zLapV) {
        long determinant = xLapU * zLapV - zLapU * xLapV;
        long reducedU = u;
        long reducedV = v;

        if (determinant != 0L) {
            long xLaps = Math.floorDiv(zLapV * u - zLapU * v, determinant);
            long zLaps = Math.floorDiv(xLapU * v - xLapV * u, determinant);
            reducedU = u - xLaps * xLapU - zLaps * zLapU;
            reducedV = v - xLaps * xLapV - zLaps * zLapV;
        } else if (xLapU != 0L) {
            long laps = Math.floorDiv(xLapU * u + xLapV * v, xLapU * xLapU + xLapV * xLapV);
            reducedU = u - laps * xLapU;
            reducedV = v - laps * xLapV;
        } else if (zLapV != 0L) {
            long laps = Math.floorDiv(zLapU * u + zLapV * v, zLapU * zLapU + zLapV * zLapV);
            reducedU = u - laps * zLapU;
            reducedV = v - laps * zLapV;
        }

        return p(permutations, reducedU + p(permutations, reducedV)) % GRADIENT_COUNT;
    }

    private static double cornerNoise(int gradientIndex, double x, double z) {
        double falloff = KERNEL_RADIUS_SQR - x * x - z * z;
        if (falloff < 0.0) {
            return 0.0;
        }

        falloff *= falloff;
        int[] gradient = PeriodicNoiseSampler.GRADIENT[gradientIndex];
        return falloff * falloff * (gradient[0] * x + gradient[1] * z);
    }

    private static int p(byte[] permutations, long index) {
        return permutations[(int) (index & PERMUTATION_MASK)] & PERMUTATION_MASK;
    }

    private PeriodicSimplexSampler() {
    }
}
