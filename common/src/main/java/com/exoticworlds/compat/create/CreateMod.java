package com.exoticworlds.compat.create;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;

public final class CreateMod {
    private static final ModPresence GATE = ModPresence.of(LogUtils.getLogger(),
            "com/simibubi/create/Create.class",
            "[create-compat] gate create_present");

    public static boolean present() {
        return GATE.present();
    }

    private CreateMod() {
    }
}
