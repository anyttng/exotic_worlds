package com.exoticworlds.compat.eminus.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.eminus.gpu.Location;
import com.eminus.gpu.pipeline.PipelineSpec;
import com.exoticworlds.compat.eminus.EminusInjectionTargets;
import com.exoticworlds.compat.eminus.LapClip;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

@Mixin(targets = "com.eminus.gpu.pipeline.PipelineSpec")
public class PipelineSpecMixin {
    @ModifyReturnValue(method = "builder" + EminusInjectionTargets.PIPELINE_SPEC_BUILDER, at = @At("RETURN"))
    private static PipelineSpec.Builder exotic_worlds$clipToTheLap(PipelineSpec.Builder builder,
            @Local(argsOnly = true, ordinal = 2) Location fragmentShader) {
        return LapClip.clipped(builder, fragmentShader);
    }
}
