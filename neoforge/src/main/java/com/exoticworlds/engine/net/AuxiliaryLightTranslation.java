package com.exoticworlds.engine.net;

import com.exoticworlds.api.v1.net.PacketRewriters;

import net.neoforged.neoforge.network.payload.AuxiliaryLightDataPayload;

public final class AuxiliaryLightTranslation {
    public static void register() {
        PacketRewriters.registerClientboundPayload(AuxiliaryLightDataPayload.class, (payload, context) ->
                new AuxiliaryLightDataPayload(context.toClient(payload.pos()), payload.entries()));
    }

    private AuxiliaryLightTranslation() {
    }
}
