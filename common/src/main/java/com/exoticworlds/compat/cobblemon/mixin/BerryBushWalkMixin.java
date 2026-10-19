package com.exoticworlds.compat.cobblemon.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.cobblemon.mod.common.entity.pokemon.ai.tasks.MoveToSweetBerryBushTask;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.LivingEntity;

@Mixin(MoveToSweetBerryBushTask.class)
public class BerryBushWalkMixin {
    @WrapOperation(
            method = "*",
            require = 2,
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_CLOSER_TO_CENTER_THAN))
    private static boolean exotic_worlds$bushRangeThroughSeam(BlockPos bush, Position body, double distance,
            Operation<Boolean> original, @Local(argsOnly = true) LivingEntity entity) {
        return SeamRange.closerToCenterThan(entity, bush, body, distance);
    }
}
