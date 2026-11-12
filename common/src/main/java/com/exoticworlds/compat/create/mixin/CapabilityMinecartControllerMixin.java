package com.exoticworlds.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.content.contraptions.minecart.capability.CapabilityMinecartController;
import com.exoticworlds.compat.create.CreateSeamFold;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin(CapabilityMinecartController.class)
public class CapabilityMinecartControllerMixin {
    @WrapOperation(method = "handleKilledMinecart",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;add(Lnet/minecraft/world/phys/Vec3;)"
                            + "Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 exotic_worlds$dropBetweenTheNearCopies(Vec3 position, Vec3 removedPosition,
            Operation<Vec3> original, @Local(argsOnly = true) Level world) {
        return original.call(position, CreateSeamFold.nearestCopy(world, position, removedPosition));
    }
}
