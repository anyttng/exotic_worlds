package com.exoticworlds.engine.noise;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.DeckGroupFold;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WrapDomain;

class DomainWarpTest {
    private static final int CHUNK_WIDTH = 32;

    private static final double XZ_SCALE = 0.25;

    private static final double SHIFT = 3.0;

    private static final int BLOCK = 137;

    private static final double[] FACTORS = {1.0, 2.0, 6.24};

    private static final WorldFold FOLD = WorldFolds.of(FlatShape.torus(WorldLoopBounds.ofWidth(CHUNK_WIDTH)));

    private static final TranslationLattice LATTICE = FOLD.blockLattice();

    private static final WrapDomain DOMAIN = LATTICE.x();

    private static final WorldFold SKEWED = new DeckGroupFold(FlatShape.latticeTorus(WorldLoopBounds.ofWidth(CHUNK_WIDTH),
            7));

    @Test
    void warpDisplacementIsTheSameCellCountAtEveryCompressionFactor() {
        for (double factor : FACTORS) {
            double scale = XZ_SCALE * factor;
            long period = PeriodicNoiseSampler.period(DOMAIN, scale, LapFloor.of(FOLD));

            assertEquals(SHIFT, cells(SHIFT, scale, period) - cells(0.0, scale, period), SHIFT / period,
                    "compression factor " + factor);
        }
    }

    @Test
    void theWarpedCoordinateStillClosesOnALap() {
        for (double factor : FACTORS) {
            double scale = XZ_SCALE * factor;

            assertEquals(DomainWarp.applyX(LATTICE, BLOCK, BLOCK, SHIFT, scale),
                    DomainWarp.applyX(LATTICE, BLOCK + DOMAIN.domainLength, BLOCK, SHIFT, scale),
                    "compression factor " + factor);
        }
    }

    @Test
    void theWarpedCoordinateClosesOnBothSkewedDeckGenerators() {
        TranslationLattice skewed = SKEWED.blockLattice();
        int width = skewed.x().domainLength;
        int height = skewed.z().domainLength;
        for (double factor : FACTORS) {
            double scale = XZ_SCALE * factor;
            double x = DomainWarp.applyX(skewed, BLOCK, BLOCK, SHIFT, scale);
            double z = DomainWarp.applyZ(skewed, BLOCK, SHIFT, scale);

            assertEquals(x, DomainWarp.applyX(skewed, BLOCK + width, BLOCK, SHIFT, scale));
            assertEquals(x, DomainWarp.applyX(skewed, BLOCK + skewed.skew(), BLOCK + height, SHIFT, scale));
            assertEquals(z, DomainWarp.applyZ(skewed, BLOCK + height, SHIFT, scale));
        }
    }

    private static double cells(double shift, double scale, long period) {
        return PeriodicNoiseSampler.foldAndScale(DOMAIN, period, scale,
                DomainWarp.applyX(LATTICE, BLOCK, BLOCK, shift, scale));
    }
}
