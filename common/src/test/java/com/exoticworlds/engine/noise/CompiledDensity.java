package com.exoticworlds.engine.noise;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunctionCompiler;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.synth.Noise;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

final class CompiledDensity {
    private static final String NOISELESS = "a noiseless density function draws no noise and no random";

    private static final DensityFunction.CompileContext NOISELESS_CONTEXT = new DensityFunction.CompileContext() {
        @Override
        public Noise createNoiseSampler(Holder<NormalNoise> parameters) {
            throw new UnsupportedOperationException(NOISELESS);
        }

        @Override
        public RandomSource createRandom(Identifier seed) {
            throw new UnsupportedOperationException(NOISELESS);
        }

        @Override
        @Deprecated
        public RandomSource createEndIslandRandom() {
            throw new UnsupportedOperationException(NOISELESS);
        }
    };

    static float valueAt(DensityFunction function, int blockX, int blockY, int blockZ) {
        return new DensityFunctionCompiler(NOISELESS_CONTEXT).getSampler(function)
                .sampleValue(SamplerContext.EMPTY_UNCACHED, blockX, blockY, blockZ);
    }

    private CompiledDensity() {
    }
}
