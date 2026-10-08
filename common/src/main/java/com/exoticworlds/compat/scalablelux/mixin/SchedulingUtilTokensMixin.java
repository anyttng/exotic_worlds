package com.exoticworlds.compat.scalablelux.mixin;

import java.util.ArrayList;
import java.util.function.IntFunction;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.scalablelux.LightLockFolds;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

// C2ME @Overwrites scheduleTask at priority 1000; applying below that loses this wrap under C2ME.
@Pseudo
@Mixin(targets = "ca.spottedleaf.starlight.common.thread.SchedulingUtil", remap = false, priority = 1200)
public class SchedulingUtilTokensMixin {
    @WrapOperation(
            method = "scheduleTask",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/ArrayList;toArray(Ljava/util/function/IntFunction;)[Ljava/lang/Object;"))
    private static Object[] toroidal$dropDuplicateTokens(
            ArrayList<Object> tokens,
            IntFunction<Object[]> generator,
            Operation<Object[]> original) {
        return original.call(LightLockFolds.distinct(tokens), generator);
    }
}
