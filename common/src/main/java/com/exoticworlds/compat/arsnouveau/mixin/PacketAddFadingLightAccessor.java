package com.exoticworlds.compat.arsnouveau.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.hollingsworth.arsnouveau.common.network.PacketAddFadingLight;

@Mixin(value = PacketAddFadingLight.class, remap = false)
public interface PacketAddFadingLightAccessor {
    @Accessor("x")
    double exotic_worlds$x();

    @Accessor("y")
    double exotic_worlds$y();

    @Accessor("z")
    double exotic_worlds$z();
}
