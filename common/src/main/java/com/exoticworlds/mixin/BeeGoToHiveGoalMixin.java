package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.core.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.bee.Bee;

@Mixin(targets = "net.minecraft.world.entity.animal.bee.Bee$BeeGoToHiveGoal")
public class BeeGoToHiveGoalMixin {
    @Shadow(aliases = "this$0")
    @Final
    private Bee bee;

    @ModifyExpressionValue(
            method = "hasReachedTarget",
            at = @At(value = "INVOKE", target = InjectionTargets.PATH_GET_TARGET))
    private BlockPos exotic_worlds$pathTargetCanonical(BlockPos target) {
        return WorldLoopAttachments.foldOf(this.bee.level()).fold(target);
    }
}
