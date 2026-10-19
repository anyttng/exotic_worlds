package com.exoticworlds.compat.cobblemon.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.cobblemon.mod.common.entity.pokemon.ai.tasks.HarvestSweetBerryBushTask;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamSteering;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;

@Mixin(HarvestSweetBerryBushTask.class)
public class BerryBushHarvestMixin {
    @WrapOperation(
            method = "checkExtraStartConditions",
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_EQUALS))
    private boolean exotic_worlds$standingOnBushThroughSeam(BlockPos body, Object bush, Operation<Boolean> original,
            @Local(argsOnly = true) LivingEntity entity) {
        return original.call(body, bush instanceof BlockPos remembered ? SeamSteering.nearestCopy(entity, remembered)
                : bush);
    }
}
