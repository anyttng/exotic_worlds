package com.exoticworlds.compat.apprenticecodex.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import jp.aquafactory.apprenticecodex.network.packet.SenseEvilHighlightsPacket;

@Mixin(value = SenseEvilHighlightsPacket.class, remap = false)
public interface SenseEvilHighlightsPacketAccessor {
    @Accessor("targets")
    List<SenseEvilHighlightsPacket.TargetData> exotic_worlds$getTargets();
}
