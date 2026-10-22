package com.exoticworlds.compat.arsnouveau.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamAim;
import com.hollingsworth.arsnouveau.common.entity.EntityAllyVex;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;

@Mixin(targets = "com.hollingsworth.arsnouveau.common.entity.EntityAllyVex$MoveHelperController", remap = false)
public abstract class EntityAllyVexMoveHelperControllerMixin {
    @Shadow(aliases = "this$0")
    @Final
    private EntityAllyVex vex;

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = InjectionTargets.LIVING_ENTITY_GET_X))
    private double exotic_worlds$faceTargetX(LivingEntity target, Operation<Double> original) {
        return SeamAim.nearestCoord(this.vex, target, Direction.Axis.X, original.call(target));
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = InjectionTargets.LIVING_ENTITY_GET_Z))
    private double exotic_worlds$faceTargetZ(LivingEntity target, Operation<Double> original) {
        return SeamAim.nearestCoord(this.vex, target, Direction.Axis.Z, original.call(target));
    }
}
