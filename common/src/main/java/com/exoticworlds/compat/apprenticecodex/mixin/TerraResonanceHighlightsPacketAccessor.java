package com.exoticworlds.compat.apprenticecodex.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import jp.aquafactory.apprenticecodex.network.packet.TerraResonanceHighlightsPacket;

import net.minecraft.core.BlockPos;

@Mixin(value = TerraResonanceHighlightsPacket.class, remap = false)
public interface TerraResonanceHighlightsPacketAccessor {
    @Accessor("targets")
    List<BlockPos> exotic_worlds$getTargets();
}
