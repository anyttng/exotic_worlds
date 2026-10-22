package com.exoticworlds.compat.arsnouveau.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.hollingsworth.arsnouveau.api.particle.ParticleEmitter;
import com.hollingsworth.arsnouveau.common.network.PacketPrestidigitation;

@Mixin(value = PacketPrestidigitation.class, remap = false)
public interface PacketPrestidigitationAccessor {
    @Accessor("emitter")
    ParticleEmitter exotic_worlds$emitter();
}
