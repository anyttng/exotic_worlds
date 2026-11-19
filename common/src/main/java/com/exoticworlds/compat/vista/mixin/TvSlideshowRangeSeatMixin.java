package com.exoticworlds.compat.vista.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;

@Mixin(targets = "net.mehvahdjukaar.vista.common.tv.TVBlockEntity")
public class TvSlideshowRangeSeatMixin {
    @WrapOperation(method = "pushSlideshowMaps",
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_DIST_SQR))
    private double exotic_worlds$tvNearestPlayer(BlockPos player, Vec3i tv, Operation<Double> original,
            @Local(argsOnly = true) Level level) {
        return original.call(player, SeamRange.toward(level, player, tv));
    }
}
