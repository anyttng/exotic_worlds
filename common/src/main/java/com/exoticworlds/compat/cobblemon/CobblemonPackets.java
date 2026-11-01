package com.exoticworlds.compat.cobblemon;

public final class CobblemonPackets {
    public static void register() {
        if (CobblemonMixinPlugin.COBBLEMON.admitted()) {
            EffectPacketRewriters.register();
        }
    }

    private CobblemonPackets() {
    }
}
