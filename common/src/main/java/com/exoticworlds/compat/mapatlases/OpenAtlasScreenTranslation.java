package com.exoticworlds.compat.mapatlases;

import java.util.Optional;

import com.exoticworlds.compat.mapatlases.mixin.OpenAtlasScreenPacketAccessor;
import com.exoticworlds.engine.net.PacketTranslator;
import com.exoticworlds.engine.net.TranslationContext;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import pepjebs.mapatlases.networking.C2S2COpenAtlasScreenPacket;

public final class OpenAtlasScreenTranslation {
    public static void register() {
        if (MapAtlasesMixinPlugin.MAP_ATLASES.isAdmitted()) {
            PacketTranslator.registerClientboundPayloadRewriter(C2S2COpenAtlasScreenPacket.class,
                    OpenAtlasScreenTranslation::seatLectern);
        }
    }

    private static CustomPacketPayload seatLectern(C2S2COpenAtlasScreenPacket sent, TranslationContext context) {
        OpenAtlasScreenPacketAccessor fields = (OpenAtlasScreenPacketAccessor) sent;
        return movedLectern(fields.exotic_worlds$getLecternPos(), context)
                .<CustomPacketPayload>map(seated -> new C2S2COpenAtlasScreenPacket(Optional.of(seated),
                        fields.exotic_worlds$isPinOnly()))
                .orElse(sent);
    }

    static Optional<BlockPos> movedLectern(Optional<BlockPos> canonical, TranslationContext context) {
        return canonical.map(context::toClient).filter(seated -> !seated.equals(canonical.get()));
    }

    private OpenAtlasScreenTranslation() {
    }
}
