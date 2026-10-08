package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.engine.seam.SeamAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.frog.ShootTongue;
import net.minecraft.world.phys.Vec3;

@Mixin(ShootTongue.class)
public class ShootTongueMixin {
    @WrapOperation(
            method = "tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/animal/frog/Frog;J)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;vectorTo(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 toroidal$tonguePullThroughSeam(Vec3 from, Vec3 to, Operation<Vec3> original,
            @Local(argsOnly = true) Frog body) {
        return SeamAim.foldDelta(body, original.call(from, to));
    }
}
