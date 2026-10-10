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
import net.minecraft.world.level.levelgen.synth.NormalNoise;

class ContextScaledNoiseTest {
    private static final int SAMPLES = 64;

    private static final int UNINDEXABLE_FIRST_OCTAVE = 36;

    private static final double[] SCALES = {NoiseConstants.UNSCALED, NoiseConstants.BADLANDS_PILLAR_SCALE};

    private static final NormalNoise UNINDEXABLE =
            NormalNoise.create(new LegacyRandomSource(SEED), UNINDEXABLE_FIRST_OCTAVE, 1.0);

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
                        double expected = UNINDEXABLE.getValue(lattice.foldX(block[0], block[1]) * scale, y,
                                lattice.foldZ(block[1]) * scale);
                        double folded = GenerationTransformerContext.withTransformer(fold,
                                () -> ContextScaledNoise.sample(GenerationTransformerContext.context(), UNINDEXABLE,
                                        block[0], y, block[1], scale));
                        assertEquals(expected, folded, "vanilla at the folded block in " + fold + " at scale "
                                + scale + " at (" + block[0] + ", " + y + ", " + block[1] + ")");
                    }
                }
            }
        }
    }
}
