package com.exoticworlds.compat.supplementaries.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.core.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

@Mixin(targets = "net.mehvahdjukaar.supplementaries.common.misc.block_movement.PistonAttemptsTracker")
public class PistonCooperatorSeatMixin {
    @ModifyExpressionValue(method = "getCooperators",
            at = @At(value = "INVOKE", target = "Ljava/util/Map$Entry;getKey()Ljava/lang/Object;"))
    private static Object exotic_worlds$candidateNearestPiston(Object candidate, @Local(argsOnly = true) ServerLevel level,
            @Local(argsOnly = true) BlockPos pistonPos) {
        return WorldLoopAttachments.foldOf(level).nearestCopy(pistonPos, (BlockPos) candidate);
    }
}
