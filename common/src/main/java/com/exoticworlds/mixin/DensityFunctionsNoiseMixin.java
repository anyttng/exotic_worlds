package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.exoticworlds.engine.noise.ContextScaledNoise;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
import com.exoticworlds.engine.noise.GenerationTransformerContext.Context;
import com.exoticworlds.engine.noise.NoiseScaleLadder;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.level.levelgen.DensityFunction;

@Mixin(targets = "net.minecraft.world.level.levelgen.DensityFunctions$Noise")
public class DensityFunctionsNoiseMixin {
    @Shadow
    @Final
    private DensityFunction.NoiseHolder noise;

    @Shadow
    @Final
    private double xzScale;

    @Shadow
    @Final
    private double yScale;

    @WrapMethod(method = "compute(Lnet/minecraft/world/level/levelgen/DensityFunction$FunctionContext;)D")
    private double toroidal$periodicCompute(DensityFunction.FunctionContext context, Operation<Double> original) {
        Context generation = GenerationTransformerContext.context();
        if (!generation.transformer().isWrapped()) {
            return original.call(context);
        }

        double xzScale = NoiseScaleLadder.installedScale(this.noise, this.xzScale);
        return ContextScaledNoise.sample(generation, this.noise,
                context.blockX(), context.blockY() * this.yScale, context.blockZ(), xzScale,
                GenerationTransformerContext.verticalShare(xzScale, this.yScale));
    }
}
