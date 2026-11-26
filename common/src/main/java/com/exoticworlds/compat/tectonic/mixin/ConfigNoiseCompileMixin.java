package com.exoticworlds.compat.tectonic.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.NoiseNode;
import com.exoticworlds.compat.tectonic.ConfigNoiseSamplers;
import com.exoticworlds.engine.noise.FoldedCompileContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import dev.worldgen.tectonic.worldgen.densityfunction.ConfigNoise;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

@Mixin(ConfigNoise.class)
public class ConfigNoiseCompileMixin implements NoiseNode {
    @Override
    public Holder<NormalNoise> exotic_worlds$getNoise() {
        return ((ConfigNoise) (Object) this).noise();
    }

    @WrapMethod(method = InjectionTargets.DENSITY_FUNCTION_COMPILE_SAMPLER)
    private DensitySampler exotic_worlds$compileFolded(DensityFunction.CompileContext context,
            Operation<DensitySampler> original) {
        FoldedCompileContext folded = FoldedCompileContext.of(context);
        if (folded == null) {
            return original.call(context);
        }

        return ConfigNoiseSamplers.compile(folded, (ConfigNoise) (Object) this);
    }
}
