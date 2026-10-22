package com.exoticworlds.compat.arsnouveau;

public final class ArsNouveauPackets {
    public static void register() {
        if (ArsNouveauMod.present()) {
            PlacementPacketRewriters.register();
            EffectPacketRewriters.register();
            EmitterPacketRewriters.register();
        }
    }

    private ArsNouveauPackets() {
    }
}
