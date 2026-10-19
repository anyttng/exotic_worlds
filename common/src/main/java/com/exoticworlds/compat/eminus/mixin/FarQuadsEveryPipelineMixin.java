package com.exoticworlds.compat.eminus.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.eminus.gpu.pipeline.PipelineSpec;
import com.exoticworlds.compat.eminus.LapClip;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

@Mixin(targets = "com.eminus.client.render.far.FarQuads")
public class FarQuadsEveryPipelineMixin {
    @ModifyReturnValue(
            method = "pipeline(Lcom/eminus/gpu/Location;Lcom/eminus/gpu/Location;FLcom/eminus/gpu/Capabilities;"
                    + "Lcom/eminus/model/port/VariantDraw;Z)Lcom/eminus/gpu/pipeline/PipelineSpec$Builder;",
            at = @At("RETURN"))
    private static PipelineSpec.Builder exotic_worlds$clipToTheLap(PipelineSpec.Builder builder) {
        return LapClip.clipped(builder);
    }
}
