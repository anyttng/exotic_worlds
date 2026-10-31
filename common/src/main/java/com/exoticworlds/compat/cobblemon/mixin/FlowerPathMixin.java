package com.exoticworlds.compat.cobblemon.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.cobblemon.mod.common.entity.pokemon.ai.tasks.PathToFlowerTask;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

@Mixin(PathToFlowerTask.class)
public class FlowerPathMixin {
    @WrapOperation(method = "*", at = @At(value = "INVOKE", target = InjectionTargets.VEC3_DISTANCE_TO_SQR_XYZ))
    private static double exotic_worlds$flowerDistanceThroughSeam(Vec3 flower, double x, double y, double z,
            Operation<Double> original, @Local(argsOnly = true) LivingEntity entity) {
        Vec3 near = SeamRange.toward(entity, flower, new Vec3(x, y, z));
        return original.call(flower, near.x, near.y, near.z);
    }
}
