package com.exoticworlds.compat.aeronautics;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.gate.ModPresence;

public final class OffroadMod {
    private static final String OFFROAD_PACKAGE = "dev/ryanhcode/offroad/";

    static final ModPresence GATE = ModPresence.gate(LogUtils.getLogger(), "[ew/aeronautics-compat] gate offroad_present")
            .probing("dev/ryanhcode/offroad/Offroad.class")
            .checking(AeronauticsMixinPlugin.CONFIG, AeronauticsMixinPlugin.OFFROAD_MIXINS::contains)
            .modPackages(OFFROAD_PACKAGE)
            .scanning(name -> false)
            .build();

    public static boolean admitted() {
        return GATE.admitted();
    }

    private OffroadMod() {
    }
}
