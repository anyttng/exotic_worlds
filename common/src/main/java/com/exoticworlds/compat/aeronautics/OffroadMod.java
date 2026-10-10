package com.exoticworlds.compat.aeronautics;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;

public final class OffroadMod {
    static final ModPresence GATE = ModPresence.gate(LogUtils.getLogger(), "[aeronautics-compat] gate offroad_present")
            .probing("dev/ryanhcode/offroad/Offroad.class")
            .checking(AeronauticsMixinPlugin.CONFIG, AeronauticsMixinPlugin.OFFROAD_MIXINS::contains)
            .build();

    public static boolean present() {
        return GATE.present();
    }

    private OffroadMod() {
    }
}
