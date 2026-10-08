package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.engine.seam.SeamAim;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;

@Mixin(AbstractMinecart.class)
public class MinecartPushMixin {
    @Unique
    private static final String toroidal$PUSH = "push(Lnet/minecraft/world/entity/Entity;)V";

    @ModifyVariable(method = toroidal$PUSH, at = @At("STORE"), ordinal = 0)
    private double toroidal$shoveDeltaX(double deltaX) {
        return SeamAim.foldX((Entity) (Object) this, deltaX);
    }

    @ModifyVariable(method = toroidal$PUSH, at = @At("STORE"), ordinal = 1)
    private double toroidal$shoveDeltaZ(double deltaZ) {
        return SeamAim.foldZ((Entity) (Object) this, deltaZ);
    }

    @ModifyVariable(method = toroidal$PUSH, at = @At("STORE"), ordinal = 4)
    private double toroidal$otherCartDeltaX(double deltaX) {
        return SeamAim.foldX((Entity) (Object) this, deltaX);
    }

    @ModifyVariable(method = toroidal$PUSH, at = @At("STORE"), ordinal = 5)
    private double toroidal$otherCartDeltaZ(double deltaZ) {
        return SeamAim.foldZ((Entity) (Object) this, deltaZ);
    }
}
