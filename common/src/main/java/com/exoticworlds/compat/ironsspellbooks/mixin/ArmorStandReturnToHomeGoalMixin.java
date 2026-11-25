package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import io.redspace.ironsspellbooks.entity.mobs.wizards.cursed_armor_stand.ArmorStandReturnToHomeGoal;
import io.redspace.ironsspellbooks.entity.mobs.wizards.cursed_armor_stand.CursedArmorStandEntity;

import net.minecraft.world.phys.Vec3;

@Mixin(value = ArmorStandReturnToHomeGoal.class, remap = false)
public abstract class ArmorStandReturnToHomeGoalMixin {
    private static final String SPAWN =
            "Lio/redspace/ironsspellbooks/entity/mobs/wizards/cursed_armor_stand/CursedArmorStandEntity;"
                    + "spawn:Lnet/minecraft/world/phys/Vec3;";

    @Shadow
    CursedArmorStandEntity mob;

    @WrapOperation(method = "tick()V", at = @At(value = "INVOKE", target = InjectionTargets.VEC3_DISTANCE_TO_SQR))
    private double exotic_worlds$stuckThroughSeam(Vec3 lastStuck, Vec3 current, Operation<Double> original) {
        return original.call(SeamSteering.nearestCopy(this.mob, lastStuck), current);
    }

    // The first read is the unset test against null, which the fold cannot take.
    @ModifyExpressionValue(method = "tick()V", at = {
            @At(value = "FIELD", target = SPAWN, opcode = Opcodes.GETFIELD, ordinal = 1),
            @At(value = "FIELD", target = SPAWN, opcode = Opcodes.GETFIELD, ordinal = 2),
            @At(value = "FIELD", target = SPAWN, opcode = Opcodes.GETFIELD, ordinal = 3)})
    private Vec3 exotic_worlds$approachNearestSpawn(Vec3 spawn) {
        return SeamSteering.nearestCopy(this.mob, spawn);
    }
}
