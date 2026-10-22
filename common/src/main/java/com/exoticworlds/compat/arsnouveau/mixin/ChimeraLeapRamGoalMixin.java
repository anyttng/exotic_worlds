package com.exoticworlds.compat.arsnouveau.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamAim;
import com.hollingsworth.arsnouveau.common.entity.WildenChimera;
import com.hollingsworth.arsnouveau.common.entity.goal.chimera.ChimeraLeapRamGoal;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;

@Mixin(value = ChimeraLeapRamGoal.class, remap = false)
public abstract class ChimeraLeapRamGoalMixin {
    @Shadow
    WildenChimera boss;

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = InjectionTargets.LIVING_ENTITY_GET_X))
    private double exotic_worlds$leapTargetX(LivingEntity target, Operation<Double> original) {
        return SeamAim.nearestCoord(this.boss, target, Direction.Axis.X, original.call(target));
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = InjectionTargets.LIVING_ENTITY_GET_Z))
    private double exotic_worlds$leapTargetZ(LivingEntity target, Operation<Double> original) {
        return SeamAim.nearestCoord(this.boss, target, Direction.Axis.Z, original.call(target));
    }
}
