package com.exoticworlds.compat.arsnouveau;

import com.exoticworlds.compat.arsnouveau.mixin.PacketPrestidigitationAccessor;
import com.exoticworlds.compat.arsnouveau.mixin.TickEmitterPacketAccessor;
import com.exoticworlds.engine.net.PacketTranslator;
import com.exoticworlds.engine.net.TranslationContext;
import com.hollingsworth.arsnouveau.api.particle.ParticleEmitter;
import com.hollingsworth.arsnouveau.common.network.PacketPrestidigitation;
import com.hollingsworth.arsnouveau.common.network.TickEmitterPacket;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

final class EmitterPacketRewriters {
    private static final int EMITTER_BUFFER_BYTES = 256;

    static void register() {
        PacketTranslator.registerClientboundPayloadRewriter(TickEmitterPacket.class, EmitterPacketRewriters::tick);
        PacketTranslator.registerClientboundPayloadRewriter(PacketPrestidigitation.class,
                EmitterPacketRewriters::prestidigitation);
    }

    static CustomPacketPayload tick(TickEmitterPacket packet, TranslationContext context) {
        ParticleEmitter emitter = ((TickEmitterPacketAccessor) packet).exotic_worlds$emitter();
        Vec3 position = emitter.getAdjustedPosition();
        Vec3 previous = previousOf(emitter, position);
        Vec3 seated = context.nearestCopy(position);
        Vec3 seatedPrevious = context.transformer().nearestCopy(seated, previous);
        if (seated.equals(position) && seatedPrevious.equals(previous)) {
            return packet;
        }

        TickEmitterPacket copy = copyOf(packet, TickEmitterPacket.CODEC, context);
        ParticleEmitter copied = ((TickEmitterPacketAccessor) copy).exotic_worlds$emitter();
        copied.position = () -> seated;
        copied.previousPosition = seatedPrevious;
        return copy;
    }

    static CustomPacketPayload prestidigitation(PacketPrestidigitation packet, TranslationContext context) {
        ParticleEmitter emitter = ((PacketPrestidigitationAccessor) packet).exotic_worlds$emitter();
        Vec3 previous = previousOf(emitter, emitter.getAdjustedPosition());
        Vec3 seatedPrevious = context.nearestCopy(previous);
        if (seatedPrevious.equals(previous)) {
            return packet;
        }

        PacketPrestidigitation copy = copyOf(packet, PacketPrestidigitation.CODEC, context);
        ((PacketPrestidigitationAccessor) copy).exotic_worlds$emitter().previousPosition = seatedPrevious;
        return copy;
    }

    private static Vec3 previousOf(ParticleEmitter emitter, Vec3 position) {
        return emitter.previousPosition == null ? position : emitter.previousPosition;
    }

    // The emitter is the server's live one, and its motion holds a back reference a new emitter would rebind.
    private static <P extends CustomPacketPayload> P copyOf(P packet, StreamCodec<RegistryFriendlyByteBuf, P> codec,
            TranslationContext context) {
        RegistryFriendlyByteBuf buffer = context.bufferFactory().apply(EMITTER_BUFFER_BYTES);
        codec.encode(buffer, packet);
        return codec.decode(buffer);
    }

    private EmitterPacketRewriters() {
    }
}
