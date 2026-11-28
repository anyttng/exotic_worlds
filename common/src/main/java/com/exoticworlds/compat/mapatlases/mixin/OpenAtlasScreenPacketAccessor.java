package com.exoticworlds.compat.mapatlases.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.core.BlockPos;
import pepjebs.mapatlases.networking.C2S2COpenAtlasScreenPacket;

@Mixin(C2S2COpenAtlasScreenPacket.class)
public interface OpenAtlasScreenPacketAccessor {
    @Accessor("lecternPos")
    Optional<BlockPos> exotic_worlds$getLecternPos();

    @Accessor("pinOnly")
    boolean exotic_worlds$isPinOnly();
}
