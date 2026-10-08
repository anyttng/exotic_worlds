package com.exoticworlds.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.engine.seam.SeamAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.entity.vehicle.MinecartFurnace;

@Mixin(MinecartFurnace.class)
public class MinecartFurnaceFuelMixin {
    @Unique
    private static final String toroidal$INTERACT =
            "interact(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)"
                    + "Lnet/minecraft/world/InteractionResult;";

    @WrapOperation(
            method = toroidal$INTERACT,
            at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
                    target = "Lnet/minecraft/world/entity/vehicle/MinecartFurnace;xPush:D"))
    private void toroidal$driveAwayXThroughSeam(MinecartFurnace furnace, double pushX, Operation<Void> original) {
        original.call(furnace, SeamAim.foldX(furnace, pushX));
    }

    @WrapOperation(
            method = toroidal$INTERACT,
            at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
                    target = "Lnet/minecraft/world/entity/vehicle/MinecartFurnace;zPush:D"))
    private void toroidal$driveAwayZThroughSeam(MinecartFurnace furnace, double pushZ, Operation<Void> original) {
        original.call(furnace, SeamAim.foldZ(furnace, pushZ));
    }
}
