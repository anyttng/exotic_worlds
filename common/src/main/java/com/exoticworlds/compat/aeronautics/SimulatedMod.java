package com.exoticworlds.compat.aeronautics;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;

public final class SimulatedMod {
    static final ModPresence GATE = ModPresence.gate(LogUtils.getLogger(), "[aeronautics-compat] gate simulated_present")
            .probing("dev/simulated_team/simulated/Simulated.class")
            .checking(AeronauticsMixinPlugin.CONFIG, mixin -> !AeronauticsMixinPlugin.AERONAUTICS_MIXINS.contains(mixin)
                    && !AeronauticsMixinPlugin.OFFROAD_MIXINS.contains(mixin))
            .build();

    public static boolean present() {
        return GATE.present();
    }

    private SimulatedMod() {
    }
}
