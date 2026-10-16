package com.exoticworlds.compat.c2me.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.c2me.C2meSeamFold;
import com.ishland.c2me.base.common.scheduler.LockTokenImpl;
import com.ishland.c2me.base.common.scheduler.SchedulingManager;
import com.ishland.c2me.rewrites.chunksystem.common.ChunkLoadingContext;
import com.ishland.c2me.rewrites.chunksystem.common.statuses.VanillaWorldGenerationDelegate;
import com.ishland.flowsched.executor.LockToken;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.util.StaticCache2D;

@Mixin(VanillaWorldGenerationDelegate.class)
public class VanillaWorldGenerationDelegateMixin {
    @WrapOperation(
            method = "upgradeToThis",
            at = @At(
                    value = "INVOKE",
                    target = InjectionTargets.STATIC_CACHE_2D_CREATE))
    private StaticCache2D<GenerationChunkHolder> toroidal$foldSeamSlots(
            int centerX,
            int centerZ,
            int range,
            StaticCache2D.Initializer<GenerationChunkHolder> initializer,
            Operation<StaticCache2D<GenerationChunkHolder>> original,
            @Local(argsOnly = true) ChunkLoadingContext context) {
        return original.call(centerX, centerZ, range,
                C2meSeamFold.foldingInitializer(context, centerX, centerZ, initializer));
    }

    @WrapOperation(
            method = "runTaskWithLockArea",
            at = @At(value = "NEW", target = "com/ishland/c2me/base/common/scheduler/LockTokenImpl"))
    private static LockTokenImpl toroidal$foldLockToken(
            int ownerTag,
            long chunkKey,
            LockTokenImpl.Usage usage,
            Operation<LockTokenImpl> original,
            @Local(argsOnly = true) SchedulingManager schedulingManager) {
        return original.call(ownerTag, C2meSeamFold.lockKey(schedulingManager, chunkKey), usage);
    }

    @ModifyArg(
            method = "runTaskWithLockArea",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/ishland/c2me/base/common/scheduler/ScheduledTask;<init>(JLjava/util/function/Supplier;[Lcom/ishland/flowsched/executor/LockToken;)V"),
            index = 2)
    private static LockToken[] toroidal$dropRepeatedLockTokens(
            LockToken[] tokens,
            @Local(argsOnly = true) SchedulingManager schedulingManager) {
        return C2meSeamFold.lockTokens(schedulingManager, tokens);
    }
}
