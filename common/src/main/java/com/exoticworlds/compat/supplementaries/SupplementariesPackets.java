package com.exoticworlds.compat.supplementaries;

public final class SupplementariesPackets {
    public static void register() {
        if (SupplementariesMixinPlugin.SUPPLEMENTARIES.admitted()) {
            CooperationPacketRewriters.register();
            ParticlePacketRewriter.register();
        }
    }

    private SupplementariesPackets() {
    }
}
