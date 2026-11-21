package com.exoticworlds.compat.vista.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.vista.ZoneViewCopies;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.net.TranslationContext;
import com.exoticworlds.engine.seam.ClientPosition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;

import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

@Mixin(targets = "net.mehvahdjukaar.vista.common.chunk_tracking.ServerCameraChunkManager")
public class ZoneViewTestMixin {
    @WrapOperation(method = "sendLoadedZoneChunks", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ChunkTrackingView;isInViewDistance(II)Z"))
    private static boolean exotic_worlds$queueChunkOwedBesideZone(ChunkTrackingView view, int chunkX, int chunkZ,
            Operation<Boolean> original, @Local(argsOnly = true) ServerPlayer player,
            @Share("context") LocalRef<TranslationContext> shared) {
        if (!original.call(view, chunkX, chunkZ)) {
            return false;
        }

        if (ClientPosition.of(player).heldAreas().isEmpty()) {
            return true;
        }

        TranslationContext context = shared.get();
        if (context == null) {
            context = TranslationContext.of(player, WorldLoopAttachments.foldOf(player.serverLevel()));
            shared.set(context);
        }

        return ZoneViewCopies.owedZoneCopy(context, new ChunkPos(chunkX, chunkZ)) == null;
    }
}
