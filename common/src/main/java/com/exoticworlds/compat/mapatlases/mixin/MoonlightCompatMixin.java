package com.exoticworlds.compat.mapatlases.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.player.Player;
import pepjebs.mapatlases.integration.moonlight.MoonlightCompat;

@Mixin(MoonlightCompat.class)
public class MoonlightCompatMixin {
    @WrapOperation(
            method = "updateMarkers",
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_DIST_TO_CENTER_SQR))
    private static double exotic_worlds$markerToPlayerAcrossSeam(BlockPos marker, Position position,
            Operation<Double> original, @Local(argsOnly = true) Player player) {
        return original.call(marker, SeamRange.towardCenterOf(player, marker, position));
    }
}
