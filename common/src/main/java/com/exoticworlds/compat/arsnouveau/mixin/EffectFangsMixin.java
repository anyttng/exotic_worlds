package com.exoticworlds.compat.arsnouveau.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamAim;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectFangs;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = EffectFangs.class, remap = false)
public abstract class EffectFangsMixin {
    @WrapOperation(method = "onResolve", at = @At(value = "INVOKE", target = InjectionTargets.MTH_ATAN2))
    private double exotic_worlds$fangAngleThroughSeam(double deltaZ, double deltaX, Operation<Double> original,
            @Local(argsOnly = true) LivingEntity shooter) {
        Vec3 delta = SeamAim.foldDelta(shooter, deltaX, deltaZ);
        return original.call(delta.z, delta.x);
    }
}
