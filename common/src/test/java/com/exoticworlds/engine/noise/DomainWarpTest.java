package com.exoticworlds.engine.noise;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.fold.deck.DeckGroupFold;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.fold.TranslationLattice;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.core.world.WorldLoopBounds;
import com.exoticworlds.core.fold.WrapDomain;
import com.exoticworlds.engine.noise.periodic.LapFloor;
import com.exoticworlds.engine.noise.periodic.PeriodicNoiseSampler;

class DomainWarpTest {
    private static final int WIDTH_CHUNKS = 32;

    private static final double XZ_SCALE = 0.25;

    private static final double SHIFT_BLOCKS = 3.0;

    private static final int BLOCK = 137;

    private static final double[] FACTORS = {1.0, 2.0, 6.24};

    private static final WorldFold FOLD = WorldFolds.of(ShapeFixtures.torus(WorldLoopBounds.ofWidth(WIDTH_CHUNKS)));

    private static final TranslationLattice LATTICE = FOLD.getBlockLattice();

    private static final WrapDomain DOMAIN = LATTICE.getX();

    private static final WorldFold SKEWED = new DeckGroupFold(
            FlatShape.latticeTorus(WorldLoopBounds.ofWidth(WIDTH_CHUNKS), 7), ShapeOptions.DEFAULT);

    @Test
    void warpDisplacementIsTheSameCellCountAtEveryCompressionFactor() {
        for (double factor : FACTORS) {
            double scale = XZ_SCALE * factor;
            long period = PeriodicNoiseSampler.period(DOMAIN, scale, LapFloor.of(FOLD));

            assertEquals(SHIFT_BLOCKS, cells(SHIFT_BLOCKS, scale, period) - cells(0.0, scale, period), SHIFT_BLOCKS / period,
                    "compression factor " + factor);
        }
    }

    @Test
    void theWarpedCoordinateStillClosesOnALap() {
        for (double factor : FACTORS) {
            double scale = XZ_SCALE * factor;

            assertEquals(DomainWarp.applyX(LATTICE, BLOCK, BLOCK, SHIFT_BLOCKS, scale),
                    DomainWarp.applyX(LATTICE, BLOCK + DOMAIN.domainLength, BLOCK, SHIFT_BLOCKS, scale),
                    "compression factor " + factor);
        }
    }

    @Test
    void theWarpedCoordinateClosesOnBothSkewedDeckGenerators() {
        TranslationLattice skewed = SKEWED.getBlockLattice();
        int widthBlocks = skewed.getX().domainLength;
        int heightBlocks = skewed.getZ().domainLength;
        for (double factor : FACTORS) {
            double scale = XZ_SCALE * factor;
            double x = DomainWarp.applyX(skewed, BLOCK, BLOCK, SHIFT_BLOCKS, scale);
            double z = DomainWarp.applyZ(skewed, BLOCK, SHIFT_BLOCKS, scale);

            assertEquals(x, DomainWarp.applyX(skewed, BLOCK + widthBlocks, BLOCK, SHIFT_BLOCKS, scale));
            assertEquals(x, DomainWarp.applyX(skewed, BLOCK + skewed.getSkew(), BLOCK + heightBlocks, SHIFT_BLOCKS, scale));
            assertEquals(z, DomainWarp.applyZ(skewed, BLOCK + heightBlocks, SHIFT_BLOCKS, scale));
        }
    }

    private static double cells(double shiftBlocks, double scale, long period) {
        return PeriodicNoiseSampler.foldAndScale(DOMAIN, period, scale,
                DomainWarp.applyX(LATTICE, BLOCK, BLOCK, shiftBlocks, scale));
    }
}
