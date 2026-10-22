package com.exoticworlds.compat.arsnouveau.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.hollingsworth.arsnouveau.common.network.PacketOneShotAnimation;

@Mixin(value = PacketOneShotAnimation.class, remap = false)
public interface PacketOneShotAnimationAccessor {
    @Accessor("x")
    int exotic_worlds$x();

    @Accessor("y")
    int exotic_worlds$y();

    @Accessor("z")
    int exotic_worlds$z();

    @Accessor("arg")
    int exotic_worlds$arg();
}
