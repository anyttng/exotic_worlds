package com.exoticworlds.compat.arsnouveau.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.hollingsworth.arsnouveau.common.network.PacketANEffect;

import net.minecraft.nbt.CompoundTag;

@Mixin(value = PacketANEffect.class, remap = false)
public interface PacketANEffectAccessor {
    @Accessor("type")
    PacketANEffect.EffectType exotic_worlds$type();

    @Accessor("x")
    double exotic_worlds$x();

    @Accessor("y")
    double exotic_worlds$y();

    @Accessor("z")
    double exotic_worlds$z();

    @Accessor("particleNbt")
    CompoundTag exotic_worlds$particleNbt();

    @Accessor("args")
    int[] exotic_worlds$args();
}
