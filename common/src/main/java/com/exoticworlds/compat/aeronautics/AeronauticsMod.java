package com.exoticworlds.compat.aeronautics;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.sable.SableBodyShift;
import com.exoticworlds.compat.sable.SableMod;

public final class AeronauticsMod {
    private static final ModPresence GATE = ModPresence.of(LogUtils.getLogger(),
            "dev/eriksonn/aeronautics/Aeronautics.class",
            "[aeronautics-compat] gate aeronautics_present");

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
