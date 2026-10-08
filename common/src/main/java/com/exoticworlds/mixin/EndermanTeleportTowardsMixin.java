package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.phys.Vec3;

@Mixin(Enderman.class)
public class EndermanTeleportTowardsMixin {
    @WrapOperation(
            method = "teleportTowards(Lnet/minecraft/world/entity/Entity;)Z",
            at = @At(value = "NEW", target = InjectionTargets.VEC3_NEW))
    private Vec3 toroidal$teleportDeltaThroughSeam(double x, double y, double z, Operation<Vec3> original) {
        return SeamAim.foldDelta((Enderman) (Object) this, original.call(x, y, z));
    }
}
