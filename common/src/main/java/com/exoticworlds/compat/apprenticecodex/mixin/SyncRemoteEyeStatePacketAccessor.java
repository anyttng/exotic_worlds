package com.exoticworlds.compat.apprenticecodex.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import jp.aquafactory.apprenticecodex.network.packet.SyncRemoteEyeStatePacket;

@Mixin(value = SyncRemoteEyeStatePacket.class, remap = false)
public interface SyncRemoteEyeStatePacketAccessor {
    @Accessor("activeUntilGameTime")
    long exotic_worlds$getActiveUntilGameTime();

    @Accessor("activeDurationTicks")
    long exotic_worlds$getActiveDurationTicks();

    @Accessor("anchorX")
    double exotic_worlds$getAnchorX();

    @Accessor("anchorY")
    double exotic_worlds$getAnchorY();

    @Accessor("anchorZ")
    double exotic_worlds$getAnchorZ();

    @Accessor("anchorYaw")
    float exotic_worlds$getAnchorYaw();

    @Accessor("anchorPitch")
    float exotic_worlds$getAnchorPitch();
}
