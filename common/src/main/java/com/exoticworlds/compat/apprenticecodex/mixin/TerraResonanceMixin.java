package com.exoticworlds.compat.apprenticecodex.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import jp.aquafactory.apprenticecodex.spell.terraresonance.TerraResonance;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Mixin(value = TerraResonance.class, remap = false)
public abstract class TerraResonanceMixin {
    @WrapOperation(method = "isHitBlockWithinRange",
            at = @At(value = "INVOKE", target = InjectionTargets.AABB_DISTANCE_TO_SQR))
    private static double exotic_worlds$hitBlockRangeThroughSeam(AABB hitBlock, Vec3 eye,
            Operation<Double> original, @Local(argsOnly = true) LivingEntity entity) {
        return original.call(hitBlock, SeamRange.toward(entity, hitBlock.getCenter(), eye));
    }
}
