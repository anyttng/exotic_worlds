package com.exoticworlds.compat.immersiveoverlays.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.immersiveoverlays.CompassOverlayFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import cc.cassian.immersiveoverlays.overlay.CompassOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;

@Mixin(CompassOverlay.class)
public class CompassOverlayMixin {
    @ModifyExpressionValue(
            method = "renderGameOverlayEvent",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;blockPosition()Lnet/minecraft/core/BlockPos;"))
    private static BlockPos exotic_worlds$showFoldedPosition(BlockPos player) {
        return CompassOverlayFold.shown(WorldLoopAttachments.foldOfReader(Minecraft.getInstance().level), player);
    }

    @ModifyExpressionValue(
            method = "renderGameOverlayEvent",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/GlobalPos;pos()Lnet/minecraft/core/BlockPos;"))
    private static BlockPos exotic_worlds$seatAnchorNearPlayer(BlockPos anchor) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return anchor;
        }

        return CompassOverlayFold.anchor(WorldLoopAttachments.foldOfReader(minecraft.level),
                player.blockPosition(), anchor);
    }
}
