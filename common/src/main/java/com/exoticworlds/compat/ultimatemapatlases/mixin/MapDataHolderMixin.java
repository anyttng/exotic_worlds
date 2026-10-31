package com.exoticworlds.compat.ultimatemapatlases.mixin;

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
import net.nanaky.ultimate_map_atlases.utils.MapDataHolder;

@Mixin(MapDataHolder.class)
public class MapDataHolderMixin {
    @WrapOperation(
            method = "updateMarkers",
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_DIST_TO_CENTER_SQR))
    private double exotic_worlds$markerToPlayerAcrossSeam(BlockPos marker, Position position,
            Operation<Double> original, @Local(argsOnly = true) Player player) {
        return original.call(marker, SeamRange.towardCenterOf(player, marker, position));
    }
}
