package com.exoticworlds.engine.noise.periodic;

import com.exoticworlds.core.fold.TranslationLattice;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WrapDomain;

public sealed interface SimplexLattice {
    long MAX_LAP_PERIOD = 1L << 29;

    WorldFold fold();

    double scale();

    boolean canCarry();

    double sample(int[] permutations, double xOffset, double zOffset, double x, double z);

    static SimplexLattice of(WorldFold fold, double scale) {
        TranslationLattice lattice = fold.getBlockLattice();
        LapFloor floor = LapFloor.of(fold);
        long xPeriod = PeriodicNoiseSampler.period(lattice.getX(), scale, floor);
        long zPeriod = PeriodicNoiseSampler.period(lattice.getZ(), scale, floor);
        boolean canCarry = xPeriod <= MAX_LAP_PERIOD && zPeriod <= MAX_LAP_PERIOD;
        return lattice.isSkewed()
                ? Skewed.of(fold, scale, canCarry, lattice, xPeriod, zPeriod)
                : Plain.of(fold, scale, canCarry, lattice, xPeriod, zPeriod);
    }

    record Plain(WorldFold fold, double scale, boolean canCarry, WrapDomain xDomain, WrapDomain zDomain,
            long xPeriod, long zPeriod, double xRatio, double zRatio, double skew, double unskew,
            long xLapU, long xLapV, long zLapU, long zLapV) implements SimplexLattice {
        static Plain of(WorldFold fold, double scale, boolean canCarry, TranslationLattice lattice, long xPeriod,
                long zPeriod) {
            WrapDomain xDomain = lattice.getX();
            WrapDomain zDomain = lattice.getZ();
            long xClosed = closedPeriod(xPeriod);
            long zClosed = closedPeriod(zPeriod);
            long denominator = PeriodicSimplexSampler.skewDenominator(xClosed, zClosed);
            long numerator = PeriodicSimplexSampler.skewNumerator(denominator);

            double skew;
            double unskew;
            if (denominator == PeriodicNoiseSampler.UNBOUNDED_PERIOD) {
                skew = PeriodicSimplexSampler.VANILLA_SKEW;
                unskew = PeriodicSimplexSampler.VANILLA_UNSKEW;
            } else {
                skew = (double) numerator / denominator;
                unskew = skew / (1.0 + 2.0 * skew);
            }

            long xLapV = denominator == PeriodicNoiseSampler.UNBOUNDED_PERIOD ? 0L : numerator * (xClosed / denominator);
            long zLapU = denominator == PeriodicNoiseSampler.UNBOUNDED_PERIOD ? 0L : numerator * (zClosed / denominator);
            return new Plain(fold, scale, canCarry, xDomain, zDomain, xPeriod, zPeriod,
                    PeriodicNoiseSampler.ratio(xDomain, xPeriod), PeriodicNoiseSampler.ratio(zDomain, zPeriod),
                    skew, unskew, xClosed + xLapV, xLapV, zLapU, zClosed + zLapU);
        }

        @Override
        public double sample(int[] permutations, double xOffset, double zOffset, double x, double z) {
            double xs = PeriodicNoiseSampler.foldAndScaleSimplex(this.xDomain, this.xPeriod, this.scale, this.xRatio, x)
                    + xOffset;
            double zs = PeriodicNoiseSampler.foldAndScaleSimplex(this.zDomain, this.zPeriod, this.scale, this.zRatio, z)
                    + zOffset;
            return PeriodicSimplexSampler.kernel(permutations, xs, zs, this.skew, this.unskew,
                    this.xLapU, this.xLapV, this.zLapU, this.zLapV);
        }

        private static long closedPeriod(long period) {
            return PeriodicNoiseSampler.isClosed(period) ? period : PeriodicNoiseSampler.UNBOUNDED_PERIOD;
        }
    }

    record Skewed(WorldFold fold, double scale, boolean canCarry, TranslationLattice lattice, double widthBlocks,
            double heightBlocks, double skewBlocks, double area, long xLapU, long xLapV, long zLapU, long zLapV,
            double xLapX, double xLapZ, double zLapX, double zLapZ) implements SimplexLattice {
        static Skewed of(WorldFold fold, double scale, boolean canCarry, TranslationLattice lattice, long xPeriod,
                long zPeriod) {
            double widthBlocks = lattice.getX().domainLength;
            double heightBlocks = lattice.getZ().domainLength;
            double skewBlocks = lattice.getSkew();
            double sheared = xPeriod * skewBlocks / widthBlocks;
            double vanillaSkew = PeriodicSimplexSampler.VANILLA_SKEW;
            double vanillaUnskew = PeriodicSimplexSampler.VANILLA_UNSKEW;
            long xLapU = Math.round(xPeriod + vanillaSkew * xPeriod);
            long xLapV = Math.round(vanillaSkew * xPeriod);
            long zLapU = Math.round(sheared + vanillaSkew * (sheared + zPeriod));
            long zLapV = Math.round(zPeriod + vanillaSkew * (sheared + zPeriod));
            return new Skewed(fold, scale, canCarry, lattice, widthBlocks, heightBlocks, skewBlocks, widthBlocks * heightBlocks,
                    xLapU, xLapV, zLapU, zLapV,
                    xLapU - vanillaUnskew * (xLapU + xLapV), xLapV - vanillaUnskew * (xLapU + xLapV),
                    zLapU - vanillaUnskew * (zLapU + zLapV), zLapV - vanillaUnskew * (zLapU + zLapV));
        }

        @Override
        public double sample(int[] permutations, double xOffset, double zOffset, double x, double z) {
            double foldedX = this.lattice.foldX(x, z);
            double foldedZ = this.lattice.foldZ(z);
            double xLaps = foldedX / this.widthBlocks - foldedZ * this.skewBlocks / this.area;
            double zLaps = foldedZ / this.heightBlocks;
            double xs = xLaps * this.xLapX + zLaps * this.zLapX + xOffset;
            double zs = xLaps * this.xLapZ + zLaps * this.zLapZ + zOffset;
            return PeriodicSimplexSampler.kernel(permutations, xs, zs, PeriodicSimplexSampler.VANILLA_SKEW,
                    PeriodicSimplexSampler.VANILLA_UNSKEW, this.xLapU, this.xLapV, this.zLapU, this.zLapV);
        }
    }
}
