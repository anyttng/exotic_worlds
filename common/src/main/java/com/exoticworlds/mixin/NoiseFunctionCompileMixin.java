package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.noise.FoldedCompileContext;
import com.exoticworlds.engine.noise.FoldedSamplers;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
import com.exoticworlds.shape.climate.ClimateCompression;
import com.exoticworlds.shape.torus.CoastFields;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunctions;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.generator.NoiseFunction;
import net.minecraft.world.level.levelgen.synth.NoiseStack;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

@Mixin(NoiseFunction.class)
public class NoiseFunctionCompileMixin {
    @SuppressWarnings("deprecation")
    @WrapMethod(method = InjectionTargets.DENSITY_FUNCTION_COMPILE_SAMPLER)
    private DensitySampler toroidal$compileFolded(DensityFunction.CompileContext context,
            Operation<DensitySampler> original) {
        if (!(context instanceof FoldedCompileContext folded)) {
            return original.call(context);
        }

        NoiseFunction self = (NoiseFunction) (Object) this;
        Holder<NormalNoise> noise = self.noise();
        double xzScale = folded.ladder().separated(noise, self.xzScale());
        double yScale = self.yScale();
        double verticalShare = GenerationTransformerContext.verticalShare(xzScale, yScale);
        NoiseStack stack = FoldedSamplers.stack(folded, noise);
        double[] layerFactors = ClimateCompression.layerFactors(folded.fold(), noise, stack, xzScale, verticalShare);
        boolean coast = noise.unwrapKey().filter(CoastFields::isCoast).isPresent();
        DensityFunction zero = DensityFunctions.zero();
        if (self.shiftX().equals(zero) && self.shiftY().equals(zero) && self.shiftZ().equals(zero)) {
            return FoldedSamplers.noise(folded, stack, xzScale, yScale, layerFactors, coast);
        }

        return FoldedSamplers.shiftedNoise(folded, stack, xzScale, yScale, layerFactors, coast,
                ClimateCompression.warpDivisor(noise, folded.fold(), xzScale, verticalShare),
                self.shiftX(), self.shiftY(), self.shiftZ());
    }
}
