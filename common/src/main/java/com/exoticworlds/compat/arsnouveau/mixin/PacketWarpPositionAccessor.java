package com.exoticworlds.compat.arsnouveau.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.hollingsworth.arsnouveau.common.network.PacketWarpPosition;

@Mixin(value = PacketWarpPosition.class, remap = false)
public interface PacketWarpPositionAccessor {
    @Accessor("entityID")
    int exotic_worlds$entityId();

    @Accessor("x")
    double exotic_worlds$x();

    @Accessor("y")
    double exotic_worlds$y();

    @Accessor("z")
    double exotic_worlds$z();

    @Accessor("xRot")
    float exotic_worlds$xRot();

    @Accessor("yRot")
    float exotic_worlds$yRot();
}
