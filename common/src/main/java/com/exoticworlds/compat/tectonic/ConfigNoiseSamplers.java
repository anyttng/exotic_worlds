package com.exoticworlds.compat.tectonic;

import com.exoticworlds.engine.noise.FoldedCompileContext;
import com.exoticworlds.engine.noise.FoldedSamplers;

import dev.worldgen.tectonic.worldgen.densityfunction.ConfigNoise;
import net.minecraft.world.level.levelgen.densityfunction.DensityBuffer;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunctions;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.densityfunction.op.BinaryFunction;

public final class ConfigNoiseSamplers {
    private static final double NO_Y_SCALE = 0.0;

    public static DensitySampler compile(FoldedCompileContext context, ConfigNoise function) {
        DensitySampler value = new BinaryFunction.ConstAddSampler(new BinaryFunction.ConstMulSampler(
                shiftedBy(context, function, function.shiftX(), function.shiftZ()), function.multiplier()),
                function.offset());
        if (!function.smootherScaling()) {
            return value;
        }

        DensitySampler volume = new BinaryFunction.ConstMulSampler(shiftedBy(context, function,
                function.shiftX().mul(function.scale()), function.shiftZ().mul(function.scale())),
                function.multiplier());
        return new SmoothSampler(volume, value);
    }

    private static DensitySampler shiftedBy(FoldedCompileContext context, ConfigNoise function,
            DensityFunction shiftX, DensityFunction shiftZ) {
        return FoldedSamplers.compileNoise(context, function.noise(), function.scale(), NO_Y_SCALE, shiftX,
                DensityFunctions.zero(), shiftZ);
    }

    private record SmoothSampler(DensitySampler volume, DensitySampler value) implements DensitySampler {
        @Override
        public void sampleVolume(SamplerContext context, DensityBuffer outputBuffer, DensityVolume volume) {
            this.volume.sampleVolume(context, outputBuffer, volume);
        }

        @Override
        public float sampleValue(SamplerContext context, int blockX, int blockY, int blockZ) {
            return this.value.sampleValue(context, blockX, blockY, blockZ);
        }
    }

    private ConfigNoiseSamplers() {
    }
}
