package com.exoticworlds.compat.vista.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.vista.ZoneReseat;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.ServerPlayer;

@Mixin(targets = "net.mehvahdjukaar.vista.common.chunk_tracking.ServerCameraChunkManager")
public class ZoneReseatMixin {
    @WrapOperation(method = "onServerPlayerTick", at = @At(value = "INVOKE",
            target = "Lnet/mehvahdjukaar/vista/common/chunk_tracking/ServerCameraChunkManager;sendLoadedZoneChunks("
                    + "Lnet/minecraft/server/level/ChunkMap;Lnet/minecraft/server/level/ServerPlayer;"
                    + "Lnet/minecraft/server/level/ChunkTrackingView;)V"))
    private static void exotic_worlds$resendMovedZonesFirst(ChunkMap chunkMap, ServerPlayer player,
            ChunkTrackingView view, Operation<Void> original) {
        ZoneReseat.resendMovedZones(player);
        original.call(chunkMap, player, view);
    }
}
