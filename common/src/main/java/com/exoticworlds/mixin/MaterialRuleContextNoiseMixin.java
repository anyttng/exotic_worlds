package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.TransformerSource;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.ContextScaledNoise;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
import com.exoticworlds.engine.noise.NoiseConstants;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.level.levelgen.material.MaterialRuleContext;
import net.minecraft.world.level.levelgen.synth.Noise;

@Mixin(targets = {
        "net.minecraft.world.level.levelgen.material.MaterialRuleContext$1",
        "net.minecraft.world.level.levelgen.material.MaterialRuleContext$2"})
public class MaterialRuleContextNoiseMixin {
    @Shadow(remap = false)
    @Final
    MaterialRuleContext this$0;

    @WrapOperation(method = "getAsDouble", at = @At(value = "INVOKE", target = InjectionTargets.NOISE_GET))
    private float toroidal$blockPositionNoise(Noise noise, double x, double y, double z, Operation<Float> original) {
        WorldFold transformer = GenerationTransformerContext.carriedOrBound(
                ((TransformerSource) (Object) this.this$0).toroidal$wrappedTransformer());
        if (transformer == null) {
            return original.call(noise, x, y, z);
        }

        return ContextScaledNoise.sample(transformer, noise, NoiseConstants.UNSCALED, x, y, z);
    }
}
