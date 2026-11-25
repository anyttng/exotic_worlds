package com.exoticworlds.compat.apprenticecodex;

public final class ApprenticeCodexPackets {
    public static void register() {
        if (ApprenticeCodexMod.admitted()) {
            BlockTargetPacketRewriters.register();
        }
    }

    private ApprenticeCodexPackets() {
    }
}
