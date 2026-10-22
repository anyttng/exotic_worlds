package com.exoticworlds.compat.arsnouveau.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.engine.seam.SeamAim;
import com.hollingsworth.arsnouveau.common.mob_jar.FrogBehavior;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = FrogBehavior.class, remap = false)
public abstract class FrogBehaviorMixin {
    @WrapOperation(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/phys/Vec3;vectorTo(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 exotic_worlds$flingThroughSeam(Vec3 from, Vec3 to, Operation<Vec3> original,
            @Local LivingEntity flung) {
        return SeamAim.foldDelta(flung, original.call(from, to));
    }
}
