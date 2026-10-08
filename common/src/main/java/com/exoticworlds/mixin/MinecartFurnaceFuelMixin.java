package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamAim;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
import net.minecraft.world.phys.Vec3;

@Mixin(MinecartFurnace.class)
public class MinecartFurnaceFuelMixin {
    @ModifyExpressionValue(
            method = "addFuel(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_SUBTRACT))
    private Vec3 toroidal$driveAwayThroughSeam(Vec3 delta) {
        return SeamAim.foldDelta((Entity) (Object) this, delta);
    }
}
