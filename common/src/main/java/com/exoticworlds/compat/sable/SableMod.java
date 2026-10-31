package com.exoticworlds.compat.sable;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.core.ForeignFrames;

public final class SableMod {
    private static final String PROBE = "dev/ryanhcode/sable/Sable.class";
    private static final String SABLE_PACKAGE = "dev/ryanhcode/sable/";

    static final ModPresence GATE = ModPresence.gate(LogUtils.getLogger(), "[ew/sable-compat] gate sable_present")
            .probing(PROBE)
            .checking("exotic_worlds.compat.sable.mixins.json")
            .modPackages(SABLE_PACKAGE)
            .build();

    public static boolean admitted() {
        return GATE.admitted();
    }

    // Sable's own mixins merge into the methods ours would wrap whether or not our Sable gate admits.
    public static boolean installed() {
        return ModPresence.probe(PROBE);
    }

    public static void register() {
        if (admitted()) {
            ForeignFrames.register(new SableFrames());
        }
    }

    private SableMod() {
    }
}
