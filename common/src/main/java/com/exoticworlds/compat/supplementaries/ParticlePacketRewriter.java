package com.exoticworlds.compat.supplementaries;

import com.exoticworlds.engine.net.PacketTranslator;
import com.exoticworlds.engine.net.TranslationContext;

import net.mehvahdjukaar.supplementaries.common.network.ClientBoundParticlePacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

final class ParticlePacketRewriter {
    static void register() {
        PacketTranslator.registerClientboundPayloadRewriter(ClientBoundParticlePacket.class,
                ParticlePacketRewriter::seated);
    }

    static CustomPacketPayload seated(ClientBoundParticlePacket packet, TranslationContext context) {
        if (packet.pos == null) {
            return packet;
        }

        Vec3 seated = context.toClient(packet.pos);
        if (seated.equals(packet.pos)) {
            return packet;
        }

        Vec3 dir = packet.type == ClientBoundParticlePacket.Kind.PEARL_TELEPORT && packet.dir != null
                ? context.fold().nearestCopy(seated, packet.dir)
                : packet.dir;
        int[] extraData = packet.getExtraData().stream().mapToInt(Integer::intValue).toArray();
        return new ClientBoundParticlePacket(seated, packet.type, dir, extraData);
    }

    private ParticlePacketRewriter() {
    }
}
