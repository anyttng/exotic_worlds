package com.exoticworlds.compat.create;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.sable.SableMod;

public final class CreateMod {
    // Beside Sable's canPlayerUse overwrite Mixin refuses this wrap and drops the class's blueprint folds with it.
    private static final String BLUEPRINT_REACH_MIXIN = "BlueprintReachMixin";

    static final ModPresence GATE = ModPresence.gate(LogUtils.getLogger(), "[create-compat] gate create_present")
            .probing("com/simibubi/create/Create.class")
            .checking("exotic_worlds.compat.create.mixins.json")
            .withholding(BLUEPRINT_REACH_MIXIN, SableMod::installed)
            .build();

    public static boolean present() {
        return GATE.present();
    }

    private CreateMod() {
    }
}
