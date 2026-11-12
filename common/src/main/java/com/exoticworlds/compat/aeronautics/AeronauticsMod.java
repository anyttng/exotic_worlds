package com.exoticworlds.compat.aeronautics;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.gate.ModPresence;
import com.exoticworlds.compat.sable.SableBodyShift;
import com.exoticworlds.compat.sable.SableMod;

public final class AeronauticsMod {
    private static final String AERONAUTICS_PACKAGE = "dev/eriksonn/aeronautics/";

    static final ModPresence GATE = ModPresence.gate(LogUtils.getLogger(), "[ew/aeronautics-compat] gate aeronautics_present")
            .probing("dev/eriksonn/aeronautics/Aeronautics.class")
            .checking(AeronauticsMixinPlugin.CONFIG, AeronauticsMixinPlugin.AERONAUTICS_MIXINS::contains)
            .modPackages(AERONAUTICS_PACKAGE)
            .scanning(name -> false)
            .build();

    public static boolean admitted() {
        return GATE.admitted();
    }

    public static void register() {
        if (SimulatedMod.admitted() && SableMod.admitted()) {
            SableBodyShift.register(RopeSeamFrame::onGroupShifted);
        }
    }

    private AeronauticsMod() {
    }
}
