package com.exoticworlds.compat.c2me.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.scalablelux.LightLockFolds;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

@Pseudo
@Mixin(targets = "ca.spottedleaf.starlight.common.thread.SchedulingUtil", remap = false)
public class SchedulingUtilLockMixin {
    @WrapOperation(
            method = "scheduleTask",
            at = @At(value = "INVOKE", target = InjectionTargets.CHUNK_POS_PACK))
    private static long toroidal$foldLockKey(
            int chunkX,
            int chunkZ,
            Operation<Long> original,
            @Local(argsOnly = true, ordinal = 0) int ownerTag) {
        return LightLockFolds.foldKey(ownerTag, original.call(chunkX, chunkZ));
    }
}
