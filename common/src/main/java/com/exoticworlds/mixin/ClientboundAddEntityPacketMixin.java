package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.client.engine.SpawnPacketSeat;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.phys.Vec3;

@Mixin(ClientboundAddEntityPacket.class)
public class ClientboundAddEntityPacketMixin {
    @Shadow
    @Final
    private double x;

    @Shadow
    @Final
    private double y;

    @Shadow
    @Final
    private double z;

    @ModifyReturnValue(method = "getX", at = @At("RETURN"))
    private double exotic_worlds$seatX(double stored) {
        return SpawnPacketSeat.seated(new Vec3(stored, this.y, this.z)).x;
    }

    @ModifyReturnValue(method = "getZ", at = @At("RETURN"))
    private double exotic_worlds$seatZ(double stored) {
        return SpawnPacketSeat.seated(new Vec3(this.x, this.y, stored)).z;
    }
}
