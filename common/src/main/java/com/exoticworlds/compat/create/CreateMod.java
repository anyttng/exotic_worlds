package com.exoticworlds.compat.create;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.gate.ModPresence;
import com.exoticworlds.compat.sable.SableMod;

public final class CreateMod {
    // Beside Sable's canPlayerUse overwrite Mixin refuses this wrap and drops the class's blueprint folds with it.
    private static final String BLUEPRINT_REACH_MIXIN = "BlueprintReachMixin";
    private static final String CREATE_PACKAGE = "com/simibubi/create/";
    private static final String CATNIP_PACKAGE = "net/createmod/catnip/";
    private static final String FLYWHEEL_PACKAGE = "dev/engine_room/flywheel/";

    static final ModPresence GATE = ModPresence.gate(LogUtils.getLogger(), "[ew/create-compat] gate create_present")
            .probing("com/simibubi/create/Create.class")
            .checking("exotic_worlds.compat.create.mixins.json")
            .withholding(BLUEPRINT_REACH_MIXIN, SableMod::installed)
            .modPackages(CREATE_PACKAGE, CATNIP_PACKAGE, FLYWHEEL_PACKAGE)
            .build();

    public static boolean admitted() {
        return GATE.admitted();
    }

    private CreateMod() {
    }
}
