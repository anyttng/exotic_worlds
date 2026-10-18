package com.exoticworlds.compat.supplementaries.mixin;

import java.util.Set;
import java.util.function.BiPredicate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.FoldedCopies;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

@Mixin(targets = "net.mehvahdjukaar.supplementaries.common.misc.block_movement.PulleyCooperationData")
public class PulleyCooperatorSeatMixin {
    @ModifyArg(method = "getCooperators", index = 2, at = @At(value = "INVOKE",
            target = "Lnet/mehvahdjukaar/supplementaries/common/misc/block_movement/CooperationTable;getCooperators(Lnet/minecraft/core/BlockPos;JLjava/util/function/BiPredicate;)Ljava/util/Set;"))
    private static BiPredicate<BlockPos, Object> exotic_worlds$reachThroughSeam(BiPredicate<BlockPos, Object> isCooperator,
            @Local(argsOnly = true) Level level, @Local(argsOnly = true) BlockPos primary) {
        WorldFold fold = WorldLoopAttachments.transformerOf(level);
        return (candidate, attempt) -> isCooperator.test(fold.nearestCopy(primary, candidate), attempt);
    }

    @ModifyReturnValue(method = "getCooperators", at = @At("RETURN"))
    private static Set<BlockPos> exotic_worlds$cooperatorsNearestPrimary(Set<BlockPos> cooperators,
            @Local(argsOnly = true) Level level, @Local(argsOnly = true) BlockPos primary) {
        WorldFold fold = WorldLoopAttachments.transformerOf(level);
        return FoldedCopies.of(cooperators, candidate -> fold.nearestCopy(primary, candidate));
    }

    @ModifyVariable(method = {"wasMovedThisTick", "markMovedThisTick"}, at = @At("HEAD"), argsOnly = true)
    private static BlockPos exotic_worlds$canonicalMovedKey(BlockPos pos, @Local(argsOnly = true) Level level) {
        return WorldLoopAttachments.transformerOf(level).fold(pos);
    }
}
