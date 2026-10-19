package com.exoticworlds.compat.cobblemon;

import java.util.function.Function;

import com.cobblemon.mod.common.net.messages.client.spawn.SpawnExtraDataEntityPacket;
import com.cobblemon.mod.common.net.messages.client.spawn.SpawnGenericBedrockPacket;
import com.cobblemon.mod.common.net.messages.client.spawn.SpawnNPCPacket;
import com.cobblemon.mod.common.net.messages.client.spawn.SpawnPokeballPacket;
import com.cobblemon.mod.common.net.messages.client.spawn.SpawnPokemonPacket;
import com.exoticworlds.engine.net.PacketTranslator;
import com.exoticworlds.engine.net.TranslationContext;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;

final class SpawnPacketRewriters {
    private static final int BUFFER_CAPACITY = 256;

    static void register() {
        register(SpawnPokemonPacket.class, SpawnPokemonPacket.Companion::decode);
        register(SpawnPokeballPacket.class, SpawnPokeballPacket.Companion::decode);
        register(SpawnNPCPacket.class, SpawnNPCPacket.Companion::decode);
        register(SpawnGenericBedrockPacket.class, SpawnGenericBedrockPacket.Companion::decode);
    }

    private static <P extends SpawnExtraDataEntityPacket<?, ?>> void register(Class<P> payloadType,
            Function<RegistryFriendlyByteBuf, P> decoder) {
        PacketTranslator.registerClientboundPayloadRewriter(payloadType,
                (payload, context) -> seated(payload, context, decoder));
    }

    static <P extends SpawnExtraDataEntityPacket<?, ?>> CustomPacketPayload seated(P payload,
            TranslationContext context, Function<RegistryFriendlyByteBuf, P> decoder) {
        ClientboundAddEntityPacket spawn = payload.getVanillaSpawnPacket();
        ClientboundAddEntityPacket moved = (ClientboundAddEntityPacket) PacketTranslator.toClient(spawn, context);
        if (moved.getX() == spawn.getX() && moved.getZ() == spawn.getZ()) {
            return payload;
        }

        RegistryFriendlyByteBuf buffer = context.bufferFactory().apply(BUFFER_CAPACITY);
        payload.encode(buffer);
        P copy = decoder.apply(buffer);
        copy.setVanillaSpawnPacket(moved);
        return copy;
    }

    private SpawnPacketRewriters() {
    }
}
