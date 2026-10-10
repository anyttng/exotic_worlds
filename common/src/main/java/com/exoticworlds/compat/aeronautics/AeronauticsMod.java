package com.exoticworlds.compat.aeronautics;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.sable.SableBodyShift;
import com.exoticworlds.compat.sable.SableMod;

public final class AeronauticsMod {
    static final ModPresence GATE = ModPresence.gate(LogUtils.getLogger(), "[aeronautics-compat] gate aeronautics_present")
            .probing("dev/eriksonn/aeronautics/Aeronautics.class")
            .checking(AeronauticsMixinPlugin.CONFIG, AeronauticsMixinPlugin.AERONAUTICS_MIXINS::contains)
            .build();

    public static boolean present() {
        return GATE.present();
    }

    public static void register() {
        if (SimulatedMod.present() && SableMod.present()) {
            SableBodyShift.register(RopeSeamFrame::onGroupShifted);
        }
    }

    private AeronauticsMod() {
    }
}
