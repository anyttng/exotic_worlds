package com.exoticworlds.compat.cobblemon.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

@Mixin(targets = "com.cobblemon.mod.common.api.ai.config.task.PlaceHoneyInSaccLeavesTaskConfig$createTask$1")
public class HoneyHoverMixin {
    @WrapOperation(
            method = {"checkExtraStartConditions", "tick"},
            require = 2,
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_DISTANCE_TO))
    private double exotic_worlds$leavesDistanceThroughSeam(Vec3 from, Vec3 to, Operation<Double> original,
            @Local(argsOnly = true) LivingEntity owner) {
        return Math.sqrt(SeamRange.sqr(owner, from, to));
    }
}
