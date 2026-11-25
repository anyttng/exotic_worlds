package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import io.redspace.ironsspellbooks.network.particles.BloodSiphonParticlesPacket;

import net.minecraft.world.phys.Vec3;

@Mixin(value = BloodSiphonParticlesPacket.class, remap = false)
public interface BloodSiphonParticlesPacketAccessor {
    @Accessor("pos1")
    Vec3 exotic_worlds$getPos1();

    @Accessor("pos2")
    Vec3 exotic_worlds$getPos2();
}
