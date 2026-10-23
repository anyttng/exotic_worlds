package com.exoticworlds.compat.arsnouveau.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.TransformerHolder;
import com.exoticworlds.core.WorldLoopAttachments;
import com.hollingsworth.arsnouveau.common.entity.ScryerCamera;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.world.level.ChunkPos;

@Mixin(value = ScryerCamera.class, remap = false)
public abstract class ScryerCameraMixin {
    @WrapOperation(method = "setChunkLoadingDistance", at = @At(value = "INVOKE", target = InjectionTargets.CHUNK_TRACKING_VIEW_OF))
    private ChunkTrackingView exotic_worlds$viewThroughSeam(ChunkPos center, int viewDistance,
            Operation<ChunkTrackingView> original) {
        ChunkTrackingView view = original.call(center, viewDistance);
        ((TransformerHolder) (Object) view).exotic_worlds$setTransformer(
                WorldLoopAttachments.transformerOf(((ScryerCamera) (Object) this).level()));
        return view;
    }
}
