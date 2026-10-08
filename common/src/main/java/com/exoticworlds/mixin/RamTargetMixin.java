package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.ai.behavior.RamTarget;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.phys.Vec3;

@Mixin(RamTarget.class)
public class RamTargetMixin {
    @WrapOperation(
            method = "start(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/animal/goat/Goat;J)V",
            at = @At(value = "NEW", target = InjectionTargets.VEC3_NEW))
    private Vec3 toroidal$ramDirectionThroughSeam(double x, double y, double z, Operation<Vec3> original,
            @Local(argsOnly = true) Goat body) {
        return SeamAim.foldDelta(body, original.call(x, y, z));
    }
}
