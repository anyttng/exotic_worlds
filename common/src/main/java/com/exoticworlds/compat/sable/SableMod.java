package com.exoticworlds.compat.sable;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.core.ForeignFrames;

public final class SableMod {
    private static final String PROBE = "dev/ryanhcode/sable/Sable.class";

    static final ModPresence GATE = ModPresence.gate(LogUtils.getLogger(), "[sable-compat] gate sable_present")
            .probing(PROBE)
            .checking("exotic_worlds.compat.sable.mixins.json")
            .build();

    public static boolean present() {
        return GATE.present();
    }

    // Sable's own mixins merge into the methods ours would wrap whether or not our Sable gate admits.
    public static boolean installed() {
        return ModPresence.probe(PROBE);
    }

    public static void register() {
        if (present()) {
            ForeignFrames.register(new SableFrames());
        }
    }

    private SableMod() {
    }
}
