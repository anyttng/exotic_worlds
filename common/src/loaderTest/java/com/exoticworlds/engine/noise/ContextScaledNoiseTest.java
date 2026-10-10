package com.exoticworlds.engine.noise;

import static com.exoticworlds.engine.noise.DensityFunctionFixture.SEED;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.WORLDS;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.blockIn;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.blockY;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;

import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.Noise;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

class ContextScaledNoiseTest {
    private static final int SAMPLES = 64;

    private static final int UNINDEXABLE_BASE_OCTAVE = 32;

    private static final double[] SCALES = {NoiseConstants.UNSCALED, NoiseConstants.BADLANDS_PILLAR_SCALE};

    private static final Noise UNINDEXABLE =
            NormalNoise.createParity(UNINDEXABLE_BASE_OCTAVE, 1.0).create(new LegacyRandomSource(SEED));

    @Test
    void aNoiseWhosePeriodOverflowsTheCellIndexSamplesVanillaAtTheFoldedBlock() {
        Random random = new Random(SEED);
        for (WorldFold fold : WORLDS) {
            TranslationLattice lattice = fold.blockLattice();
            int xWidth = fold.blockDomain(Direction.Axis.X).domainLength;
            int zWidth = fold.blockDomain(Direction.Axis.Z).domainLength;
            for (double scale : SCALES) {
                for (int i = 0; i < SAMPLES; i++) {
                    int x = blockIn(random, fold.blockDomain(Direction.Axis.X));
                    int y = blockY(random);
                    int z = blockIn(random, fold.blockDomain(Direction.Axis.Z));
                    for (int[] block : new int[][] {{x, z}, {x + xWidth, z}, {x, z + zWidth}}) {
                        float expected = UNINDEXABLE.get(lattice.foldX(block[0], block[1]) * scale, y,
                                lattice.foldZ(block[1]) * scale);
                        assertEquals(expected, ContextScaledNoise.sample(fold, UNINDEXABLE, scale, block[0], y,
                                block[1]), "vanilla at the folded block in " + fold + " at scale " + scale + " at ("
                                        + block[0] + ", " + y + ", " + block[1] + ")");
                    }
                }
            }
        }
    }
}
