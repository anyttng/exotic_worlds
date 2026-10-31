package com.exoticworlds.compat.aeronautics;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;

public final class SimulatedMod {
    private static final String SIMULATED_PACKAGE = "dev/simulated_team/simulated/";
    private static final String SABLE_PACKAGE = "dev/ryanhcode/sable/";

    static final ModPresence GATE = ModPresence.gate(LogUtils.getLogger(), "[ew/aeronautics-compat] gate simulated_present")
            .probing("dev/simulated_team/simulated/Simulated.class")
            .checking(AeronauticsMixinPlugin.CONFIG, mixin -> !AeronauticsMixinPlugin.AERONAUTICS_MIXINS.contains(mixin)
                    && !AeronauticsMixinPlugin.OFFROAD_MIXINS.contains(mixin))
            .modPackages(SIMULATED_PACKAGE, SABLE_PACKAGE)
            .scanning(name -> true)
            .build();

    public static boolean admitted() {
        return GATE.admitted();
    }

    private SimulatedMod() {
    }
}
