package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import io.redspace.ironsspellbooks.network.particles.ShockwaveParticlesPacket;

import net.minecraft.world.phys.Vec3;

@Mixin(value = ShockwaveParticlesPacket.class, remap = false)
public interface ShockwaveParticlesPacketAccessor {
    @Accessor("pos")
    Vec3 exotic_worlds$getPos();

    @Accessor("radius")
    float exotic_worlds$getRadius();

    @Accessor("particleName")
    String exotic_worlds$getParticleName();
}
