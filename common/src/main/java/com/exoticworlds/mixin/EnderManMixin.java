package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamAim;

import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.phys.Vec3;

@Mixin(EnderMan.class)
public class EnderManMixin {
    @WrapOperation(
            method = "isLookingAtMe",
            at = @At(value = "NEW", target = InjectionTargets.VEC3_NEW))
    private Vec3 toroidal$gazeThroughSeam(double x, double y, double z, Operation<Vec3> original) {
        return SeamAim.foldDelta((EnderMan) (Object) this, original.call(x, y, z));
    }
}
