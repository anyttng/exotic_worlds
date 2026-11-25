package com.exoticworlds.compat.ironsspellbooks;

public final class IronsSpellbooksPackets {
    public static void register() {
        if (IronsSpellbooksMod.admitted()) {
            ParticlePacketRewriters.register();
            CastDataPacketRewriters.register();
            ParticleOptionRewriters.register();
        }
    }

    private IronsSpellbooksPackets() {
    }
}
