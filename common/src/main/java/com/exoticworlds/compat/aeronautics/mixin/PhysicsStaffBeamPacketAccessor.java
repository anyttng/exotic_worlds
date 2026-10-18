package com.exoticworlds.compat.aeronautics.mixin;

import java.util.UUID;

import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import dev.simulated_team.simulated.network.packets.physics_staff.PhysicsStaffBeamPacket;

@Mixin(value = PhysicsStaffBeamPacket.class, remap = false)
public interface PhysicsStaffBeamPacketAccessor {
    @Accessor("uuid")
    UUID exotic_worlds$uuid();

    @Accessor("start")
    Vector3d exotic_worlds$start();

    @Accessor("end")
    Vector3d exotic_worlds$end();
}
