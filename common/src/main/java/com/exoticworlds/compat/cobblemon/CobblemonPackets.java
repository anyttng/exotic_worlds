package com.exoticworlds.compat.cobblemon;

import com.exoticworlds.compat.ModPresence;

public final class CobblemonPackets {
    private static final String SPAWN_PACKET_CLASS =
            "com/cobblemon/mod/common/net/messages/client/spawn/SpawnExtraDataEntityPacket.class";

    public static void register() {
        if (ModPresence.probe(SPAWN_PACKET_CLASS)) {
            SpawnPacketRewriters.register();
            EffectPacketRewriters.register();
        }
    }

    private CobblemonPackets() {
    }
}
