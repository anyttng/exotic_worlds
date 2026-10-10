package com.exoticworlds.engine.noise;

import static com.exoticworlds.engine.noise.DensityFunctionFixture.SEED;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.blockIn;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.blockY;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WrapDomain;

import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;

class BlendedNoiseVanillaAtFoldTest {
    private static final int SAMPLES = 64;

    private static final int HALF_CHUNK_SPAN = 1 << 20;

    private static final WorldFold WIDE = WorldFolds.of(FlatShape.torus(
            new WorldLoopBounds(-HALF_CHUNK_SPAN, HALF_CHUNK_SPAN, -HALF_CHUNK_SPAN, HALF_CHUNK_SPAN)));

    private static final double EXTREME_XZ_SCALE = 1000.0;
    private static final double Y_SCALE = 0.125;
    private static final double TIGHT_XZ_FACTOR = 1.0;
    private static final double Y_FACTOR = 160.0;
    private static final double SMEAR = 8.0;

    private static final BlendedNoise EVERY_STACK_PAST_THE_INDEX = new BlendedNoise(new LegacyRandomSource(SEED),
            EXTREME_XZ_SCALE, Y_SCALE, TIGHT_XZ_FACTOR, Y_FACTOR, SMEAR);

    @Test
    void anExtremeHorizontalScaleSamplesVanillaAtTheFoldedBlock() {
        TranslationLattice lattice = WIDE.blockLattice();
        WrapDomain xDomain = WIDE.blockDomain(Direction.Axis.X);
        WrapDomain zDomain = WIDE.blockDomain(Direction.Axis.Z);
        Random random = new Random(SEED);
        for (int i = 0; i < SAMPLES; i++) {
            int x = blockIn(random, xDomain);
            int y = blockY(random);
            int z = blockIn(random, zDomain);
            for (int[] block : new int[][] {{x, z}, {x + xDomain.domainLength, z}, {x, z + zDomain.domainLength}}) {
                double expected = EVERY_STACK_PAST_THE_INDEX.compute(new DensityFunction.SinglePointContext(
                        lattice.foldX(block[0], block[1]), y, lattice.foldZ(block[1])));
                DensityFunction.FunctionContext at = new DensityFunction.SinglePointContext(block[0], y, block[1]);
                double folded = GenerationTransformerContext.withTransformer(WIDE,
                        () -> EVERY_STACK_PAST_THE_INDEX.compute(at));
                assertEquals(expected, folded,
                        "vanilla at the folded block at (" + block[0] + ", " + y + ", " + block[1] + ")");
            }
        }
    }
}
