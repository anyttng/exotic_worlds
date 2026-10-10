package com.exoticworlds.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.ContextScaledNoise;
import com.exoticworlds.engine.noise.DomainWarp.Divisor;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
import com.exoticworlds.engine.noise.GenerationTransformerContext.Context;
import com.exoticworlds.engine.noise.NoiseScaleLadder;
import com.exoticworlds.shape.climate.ClimateCompression;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.level.levelgen.DensityFunction;

@Mixin(targets = "net.minecraft.world.level.levelgen.DensityFunctions$ShiftedNoise")
public class DensityFunctionsShiftedNoiseMixin {
    @Unique
    private @Nullable Divisor toroidal$warpDivisor;

    @Shadow
    @Final
    private DensityFunction shiftX;

    @Shadow
    @Final
    private DensityFunction shiftY;

    @Shadow
    @Final
    private DensityFunction shiftZ;

    @Shadow
    @Final
    private double xzScale;

    @Shadow
    @Final
    private double yScale;

    @Shadow
    @Final
    private DensityFunction.NoiseHolder noise;

    @WrapMethod(method = "compute(Lnet/minecraft/world/level/levelgen/DensityFunction$FunctionContext;)D")
    private double toroidal$periodicCompute(DensityFunction.FunctionContext context, Operation<Double> original) {
        Context generation = GenerationTransformerContext.context();
        WorldFold transformer = generation.wrappedTransformer();
        if (transformer == null) {
            return original.call(context);
        }

        double xzScale = NoiseScaleLadder.installedScale(this.noise, this.xzScale);
        double verticalShare = GenerationTransformerContext.verticalShare(xzScale, this.yScale);
        double y = context.blockY() * this.yScale + this.shiftY.compute(context);
        if (xzScale == 0.0) {
            return ContextScaledNoise.sample(generation, this.noise, context.blockX(), y, context.blockZ(), xzScale,
                    this.xzScale, verticalShare);
        }

        return ContextScaledNoise.sampleWarped(generation, this.noise, context.blockX(), y, context.blockZ(),
                this.shiftX.compute(context), this.shiftZ.compute(context),
                this.toroidal$warpDivisor(transformer, xzScale), xzScale, this.xzScale, verticalShare);
    }

    @Unique
    private double toroidal$warpDivisor(WorldFold transformer, double xzScale) {
        Divisor divisor = this.toroidal$warpDivisor;
        if (divisor == null || divisor.fold() != transformer) {
            divisor = new Divisor(transformer, ClimateCompression.warpDivisor(this.noise, transformer,
                    xzScale, GenerationTransformerContext.verticalShare(xzScale, this.yScale)));
            this.toroidal$warpDivisor = divisor;
        }

        return divisor.value();
    }
}
