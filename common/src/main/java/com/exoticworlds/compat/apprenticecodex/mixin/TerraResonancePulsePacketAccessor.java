package com.exoticworlds.compat.apprenticecodex.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import jp.aquafactory.apprenticecodex.network.packet.TerraResonancePulsePacket;

import net.minecraft.core.Direction;

@Mixin(value = TerraResonancePulsePacket.class, remap = false)
public interface TerraResonancePulsePacketAccessor {
    @Accessor("x")
    double exotic_worlds$getX();

    @Accessor("y")
    double exotic_worlds$getY();

    @Accessor("z")
    double exotic_worlds$getZ();

    @Accessor("selectedFace")
    Direction exotic_worlds$getSelectedFace();

    @Accessor("maxRadius")
    float exotic_worlds$getMaxRadius();
}
