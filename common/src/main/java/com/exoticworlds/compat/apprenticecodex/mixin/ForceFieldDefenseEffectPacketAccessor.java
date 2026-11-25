package com.exoticworlds.compat.apprenticecodex.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import jp.aquafactory.apprenticecodex.network.packet.ForceFieldDefenseEffectPacket;

@Mixin(value = ForceFieldDefenseEffectPacket.class, remap = false)
public interface ForceFieldDefenseEffectPacketAccessor {
    @Accessor("x")
    double exotic_worlds$getX();

    @Accessor("y")
    double exotic_worlds$getY();

    @Accessor("z")
    double exotic_worlds$getZ();

    @Accessor("normalX")
    float exotic_worlds$getNormalX();

    @Accessor("normalY")
    float exotic_worlds$getNormalY();

    @Accessor("normalZ")
    float exotic_worlds$getNormalZ();

    @Accessor("sizeScale")
    float exotic_worlds$getSizeScale();

    @Accessor("lifetimeScale")
    float exotic_worlds$getLifetimeScale();

    @Accessor("renderWave")
    boolean exotic_worlds$shouldRenderWave();

    @Accessor("failed")
    boolean exotic_worlds$isFailed();

    @Accessor("absorb")
    boolean exotic_worlds$isAbsorbing();
}
