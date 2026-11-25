package com.exoticworlds.compat.apprenticecodex.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import jp.aquafactory.apprenticecodex.network.packet.ManaSiphonOrbEffectPacket;

@Mixin(value = ManaSiphonOrbEffectPacket.class, remap = false)
public interface ManaSiphonOrbEffectPacketAccessor {
    @Accessor("impactX")
    double exotic_worlds$getImpactX();

    @Accessor("impactY")
    double exotic_worlds$getImpactY();

    @Accessor("impactZ")
    double exotic_worlds$getImpactZ();

    @Accessor("ownerEntityId")
    int exotic_worlds$getOwnerEntityId();

    @Accessor("orbs")
    List<ManaSiphonOrbEffectPacket.OrbData> exotic_worlds$getOrbs();
}
