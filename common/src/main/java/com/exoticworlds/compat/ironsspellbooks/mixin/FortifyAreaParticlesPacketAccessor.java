package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import io.redspace.ironsspellbooks.network.particles.FortifyAreaParticlesPacket;

import net.minecraft.world.phys.Vec3;

@Mixin(value = FortifyAreaParticlesPacket.class, remap = false)
public interface FortifyAreaParticlesPacketAccessor {
    @Accessor("pos")
    Vec3 exotic_worlds$getPos();
}
