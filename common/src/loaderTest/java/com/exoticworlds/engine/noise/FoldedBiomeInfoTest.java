package com.exoticworlds.engine.noise;

import static com.exoticworlds.engine.noise.DensityFunctionFixture.SEED;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.WORLDS;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.blockIn;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;

import net.minecraft.core.Direction;
import net.minecraft.world.level.biome.Biome;

class FoldedBiomeInfoTest {
    private static final int SAMPLES = 64;

    private static final double UNCARRIED_FACTOR = 1.0E-8;

    @Test
    @SuppressWarnings("removal")
    void aFactorPastTheSimplexBoundSamplesVanillaAtTheFoldedBlock() {
        Random random = new Random(SEED);
        for (WorldFold fold : WORLDS) {
            TranslationLattice lattice = fold.blockLattice();
            int xWidth = fold.blockDomain(Direction.Axis.X).domainLength;
            int zWidth = fold.blockDomain(Direction.Axis.Z).domainLength;
            for (int i = 0; i < SAMPLES; i++) {
                int x = blockIn(random, fold.blockDomain(Direction.Axis.X));
                int z = blockIn(random, fold.blockDomain(Direction.Axis.Z));
                for (int[] block : new int[][] {{x, z}, {x + xWidth, z}, {x, z + zWidth}}) {
                    double expected = Biome.BIOME_INFO_NOISE.getValue(
                            lattice.foldX(block[0], block[1]) / UNCARRIED_FACTOR,
                            lattice.foldZ(block[1]) / UNCARRIED_FACTOR, false);
                    double folded = GenerationTransformerContext.withTransformer(fold,
                            () -> FoldedBiomeInfo.sample(GenerationTransformerContext.context(), fold,
                                    UNCARRIED_FACTOR, block[0], block[1]));
                    assertEquals(expected, folded,
                            "biome info at the folded block in " + fold + " at (" + block[0] + ", " + block[1] + ")");
                }
            }
        }
    }
}
