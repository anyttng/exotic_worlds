package com.exoticworlds.compat.eminus.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.eminus.client.session.ClientSession;
import com.eminus.gpu.pipeline.PipelineSpec;
import com.exoticworlds.compat.eminus.CellFold;
import com.exoticworlds.compat.eminus.CellFoldHolder;
import com.exoticworlds.compat.eminus.LapClip;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

@Mixin(targets = "com.eminus.client.render.far.FarQuads")
public class FarQuadsMixin {
    @ModifyReturnValue(
            method = "pipeline(Lcom/eminus/gpu/Location;Lcom/eminus/gpu/Location;FLcom/eminus/gpu/Capabilities;"
                    + "Lcom/eminus/model/port/VariantDraw;)Lcom/eminus/gpu/pipeline/PipelineSpec$Builder;",
            at = @At("RETURN"))
    private static PipelineSpec.Builder toroidal$clipToTheLap(PipelineSpec.Builder builder) {
        CellFold fold = CellFoldHolder.foldOf(ClientSession.runtime());
        if (!fold.repeats()) {
            return builder;
        }

        return builder.withDefine(LapClip.DEFINE)
                .withDefine(LapClip.DEFINE_X, fold.lapXBlocks())
                .withDefine(LapClip.DEFINE_Z, fold.lapZBlocks())
                .withDefine(LapClip.DEFINE_SKEW, fold.skewBlocks());
    }
}
