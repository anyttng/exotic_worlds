package com.exoticworlds.engine.noise;

import java.util.List;
import java.util.Random;

import com.exoticworlds.accessors.CoastLiftCache;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WrapDomain;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunctionCompiler;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.synth.Noise;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public final class DensityFunctionFixture {
    public static final long SEED = 0x0153EL;

    private static final int WORLD_HEIGHT = 384;
    private static final int LOWEST_Y = -64;

    private static final int FIRST_OCTAVE = -6;

    public static final Holder<NormalNoise> NOISE_DATA =
            Holder.direct(NormalNoise.createParity(FIRST_OCTAVE, 1.0, 1.0, 1.0));

    public static final int CLIMATE_FIRST_OCTAVE = -10;

    public static final DoubleList CLIMATE_AMPLITUDES = DoubleArrayList.of(1.5, 0.0, 1.0);

    public static final double CLIMATE_XZ_SCALE = 0.25;

    public static final Holder<NormalNoise> CLIMATE_NOISE_DATA =
            Holder.direct(NormalNoise.createParity(CLIMATE_FIRST_OCTAVE, CLIMATE_AMPLITUDES));

    public static final WorldFold SQUARE =
            WorldFolds.of(FlatShape.torus(new WorldLoopBounds(-16, 16, -16, 16)));

    public static final WorldFold RECTANGULAR =
            WorldFolds.of(FlatShape.torus(new WorldLoopBounds(-16, 16, -8, 8)));

    public static final List<WorldFold> WORLDS = List.of(SQUARE, RECTANGULAR);

    private static final DensityFunction.CompileContext SEEDED = new DensityFunction.CompileContext() {
        @Override
        public Noise createNoiseSampler(Holder<NormalNoise> parameters) {
            return parameters.value().create(new LegacyRandomSource(SEED));
        }

        @Override
        public RandomSource createRandom(Identifier seed) {
            return new LegacyRandomSource(SEED);
        }

        @Override
        @Deprecated
        @SuppressWarnings("InlineMeSuggester")
        public RandomSource createEndIslandRandom() {
            return new LegacyRandomSource(SEED);
        }
    };

    private static final CoastLiftCache NO_LIFT = new CoastLiftCache() {
        @Override
        public double toroidal$coastLift() {
            return 0.0;
        }

        @Override
        public void toroidal$coastLift(double lift) {
        }
    };

    public static DensitySampler compile(DensityFunction function, WorldFold fold) {
        return new DensityFunctionCompiler(new FoldedCompileContext(SEEDED, fold, NO_LIFT)).getSampler(function);
    }

    public static DensitySampler compileVanilla(DensityFunction function) {
        return new DensityFunctionCompiler(SEEDED).getSampler(function);
    }

    public static DensitySampler compileSeparated(DensityFunction function, WorldFold fold) {
        return new DensityFunctionCompiler(new FoldedCompileContext(SEEDED, fold, NO_LIFT, ladderOf(function, fold)))
                .getSampler(function);
    }

    public static NoiseScaleLadder ladderOf(DensityFunction function, WorldFold fold) {
        return NoiseScaleLadder.of(fold, List.of(function), SEEDED);
    }

    public static float sample(DensitySampler sampler, int x, int y, int z) {
        return sampler.sampleValue(SamplerContext.EMPTY_UNCACHED, x, y, z);
    }

    public static int blockIn(Random random, WrapDomain domain) {
        return domain.lowerBound + random.nextInt(domain.domainLength);
    }

    public static int blockY(Random random) {
        return LOWEST_Y + random.nextInt(WORLD_HEIGHT);
    }

    private DensityFunctionFixture() {
    }
}
