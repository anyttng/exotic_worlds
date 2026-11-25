package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import io.redspace.ironsspellbooks.network.particles.RegenCloudParticlesPacket;

import net.minecraft.world.phys.Vec3;

@Mixin(value = RegenCloudParticlesPacket.class, remap = false)
public interface RegenCloudParticlesPacketAccessor {
    @Accessor("pos")
    Vec3 exotic_worlds$getPos();
}
