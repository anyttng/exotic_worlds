package com.exoticworlds.compat.apprenticecodex.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import jp.aquafactory.apprenticecodex.network.packet.HeavenlyFistPulsePacket;

@Mixin(value = HeavenlyFistPulsePacket.class, remap = false)
public interface HeavenlyFistPulsePacketAccessor {
    @Accessor("x")
    double exotic_worlds$getX();

    @Accessor("y")
    double exotic_worlds$getY();

    @Accessor("z")
    double exotic_worlds$getZ();

    @Accessor("maxRadius")
    float exotic_worlds$getMaxRadius();
}
