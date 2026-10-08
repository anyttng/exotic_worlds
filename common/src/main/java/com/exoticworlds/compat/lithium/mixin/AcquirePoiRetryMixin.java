package com.exoticworlds.compat.lithium.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.compat.lithium.LithiumInjectionTargets;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.mixin.SectionStorageAccessor;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.AcquirePoi;
import net.minecraft.world.entity.ai.village.poi.PoiManager;

@Mixin(value = AcquirePoi.class, priority = 1100)
public class AcquirePoiRetryMixin {
    @WrapOperation(
            method = "@MixinSquared:Handler",
            at = @At(value = "INVOKE", target = LithiumInjectionTargets.DISTANCES_DISTANCE_SQ))
    @TargetHandler(
            mixin = LithiumInjectionTargets.TASKS_ACQUIRE_POI_MIXIN,
            name = LithiumInjectionTargets.RETRY_MARKER_LAMBDA)
    private static long toroidal$retryDistanceThroughSeam(BlockPos poiPos, BlockPos center, Operation<Long> original,
            @Local(argsOnly = true) PoiManager poiManager) {
        WorldFold fold = ((SectionStorageAccessor) poiManager).toroidal$getLevelHeightAccessor() instanceof ServerLevel level
                ? WorldLoopAttachments.transformerOf(level)
                : WorldFolds.NOOP;
        return original.call(fold.nearestCopy(center, poiPos), center);
    }
}
