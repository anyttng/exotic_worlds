package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import io.redspace.ironsspellbooks.network.particles.HealParticlesPacket;

import net.minecraft.world.phys.Vec3;

@Mixin(value = HealParticlesPacket.class, remap = false)
public interface HealParticlesPacketAccessor {
    @Accessor("pos")
    Vec3 exotic_worlds$getPos();
}
