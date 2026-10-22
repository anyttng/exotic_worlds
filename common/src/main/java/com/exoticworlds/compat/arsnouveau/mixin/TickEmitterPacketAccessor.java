package com.exoticworlds.compat.arsnouveau.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.hollingsworth.arsnouveau.api.particle.ParticleEmitter;
import com.hollingsworth.arsnouveau.common.network.TickEmitterPacket;

@Mixin(value = TickEmitterPacket.class, remap = false)
public interface TickEmitterPacketAccessor {
    @Accessor("emitter")
    ParticleEmitter exotic_worlds$emitter();
}
