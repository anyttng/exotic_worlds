package com.exoticworlds.compat.vista.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.accessors.FoldHolder;
import com.exoticworlds.core.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.mehvahdjukaar.vista.common.chunk_tracking.ServerExtraChunkViewData;
import net.minecraft.server.level.ServerPlayer;

@Mixin(targets = "net.mehvahdjukaar.vista.common.chunk_tracking.ServerCameraChunkManager")
public class ZoneFoldStampMixin {
    @WrapOperation(method = "refreshWatchedViewFinders", at = @At(value = "INVOKE",
            target = "Lnet/mehvahdjukaar/vista/common/chunk_tracking/ServerExtraChunkViewData;clearZones()V"))
    private static void exotic_worlds$stampLevelFold(ServerExtraChunkViewData viewData, Operation<Void> original,
            @Local(argsOnly = true) ServerPlayer player) {
        ((FoldHolder) viewData).exotic_worlds$setFold(WorldLoopAttachments.foldOf(player.serverLevel()));
        original.call(viewData);
    }
}
