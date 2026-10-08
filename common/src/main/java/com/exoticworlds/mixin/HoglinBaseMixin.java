package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.hoglin.HoglinBase;
import net.minecraft.world.phys.Vec3;

@Mixin(HoglinBase.class)
public interface HoglinBaseMixin {
    @WrapOperation(
            method = "throwTarget(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At(value = "NEW", target = InjectionTargets.VEC3_NEW))
    private static Vec3 toroidal$tossDirectionThroughSeam(double x, double y, double z, Operation<Vec3> original,
            @Local(argsOnly = true, ordinal = 0) LivingEntity body) {
        return SeamAim.foldDelta(body, original.call(x, y, z));
    }
}
