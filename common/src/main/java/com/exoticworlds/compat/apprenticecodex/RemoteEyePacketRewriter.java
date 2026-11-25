package com.exoticworlds.compat.apprenticecodex;

import com.exoticworlds.compat.apprenticecodex.mixin.SyncRemoteEyeStatePacketAccessor;
import com.exoticworlds.engine.net.PacketTranslator;
import com.exoticworlds.engine.net.TranslationContext;

import jp.aquafactory.apprenticecodex.network.packet.SyncRemoteEyeStatePacket;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

final class RemoteEyePacketRewriter {
    static void register() {
        PacketTranslator.registerClientboundPayloadRewriter(SyncRemoteEyeStatePacket.class,
                RemoteEyePacketRewriter::state);
    }

    static CustomPacketPayload state(SyncRemoteEyeStatePacket packet, TranslationContext context) {
        SyncRemoteEyeStatePacketAccessor state = (SyncRemoteEyeStatePacketAccessor) packet;
        Vec3 anchor = new Vec3(state.exotic_worlds$getAnchorX(), state.exotic_worlds$getAnchorY(),
                state.exotic_worlds$getAnchorZ());
        Vec3 seated = context.nearestCopy(anchor);
        return seated.equals(anchor)
                ? packet
                : new SyncRemoteEyeStatePacket(state.exotic_worlds$getActiveUntilGameTime(),
                        state.exotic_worlds$getActiveDurationTicks(), seated.x, seated.y, seated.z,
                        state.exotic_worlds$getAnchorYaw(), state.exotic_worlds$getAnchorPitch());
    }

    private RemoteEyePacketRewriter() {
    }
}
