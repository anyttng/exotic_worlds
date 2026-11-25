package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import io.redspace.ironsspellbooks.network.particles.FieryExplosionParticlesPacket;

import net.minecraft.world.phys.Vec3;

@Mixin(value = FieryExplosionParticlesPacket.class, remap = false)
public interface FieryExplosionParticlesPacketAccessor {
    @Accessor("pos1")
    Vec3 exotic_worlds$getPos1();

    @Accessor("radius")
    float exotic_worlds$getRadius();
}
