package com.exoticworlds.compat.supplementaries;

import com.exoticworlds.compat.ModPresence;

public final class SupplementariesPackets {
    private static final String COOPERATORS_PACKET_CLASS =
            "net/mehvahdjukaar/supplementaries/common/network/ClientBoundPistonCooperatorsPacket.class";

    public static void register() {
        if (ModPresence.probe(COOPERATORS_PACKET_CLASS)) {
            CooperationPacketRewriters.register();
        }
    }

    private SupplementariesPackets() {
    }
}
