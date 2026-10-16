package com.exoticworlds.compat.moonlight;

import com.exoticworlds.engine.net.PacketTranslator;
import com.exoticworlds.engine.net.TranslationContext;

import net.mehvahdjukaar.moonlight.core.network.ClientBoundParticleAroundBlockMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

final class ParticleMessageRewriter {
    static void register() {
        PacketTranslator.registerClientboundPayloadRewriter(ClientBoundParticleAroundBlockMessage.class,
                ParticleMessageRewriter::seated);
    }

    static CustomPacketPayload seated(ClientBoundParticleAroundBlockMessage message, TranslationContext context) {
        BlockPos seated = context.nearestCopy(message.pos);
        return seated.equals(message.pos) ? message : new ClientBoundParticleAroundBlockMessage(seated, message.type);
    }

    private ParticleMessageRewriter() {
    }
}
