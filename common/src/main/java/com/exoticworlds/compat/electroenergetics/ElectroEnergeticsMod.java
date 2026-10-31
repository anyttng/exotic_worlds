package com.exoticworlds.compat.electroenergetics;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;

public final class ElectroEnergeticsMod {
    static final String SPAWN_GUARD_MIXIN = "BulbSpawnGuardMixin";
    static final String ELECTROENERGETICS_PACKAGE = "com/george_vi/electroenergetics/";
    private static final String CREATE_PACKAGE = "com/simibubi/create/";
    private static final String CATNIP_PACKAGE = "net/createmod/catnip/";
    private static final String FLYWHEEL_PACKAGE = "dev/engine_room/flywheel/";

    static final ModPresence GATE = ModPresence.gate(LogUtils.getLogger(),
                    "[ew/electroenergetics-compat] gate electroenergetics_present")
            .probing("com/george_vi/electroenergetics/CreateElectroEnergetics.class")
            .checking(ElectroEnergeticsMixinPlugin.CONFIG, mixin -> !mixin.equals(SPAWN_GUARD_MIXIN))
            .modPackages(ELECTROENERGETICS_PACKAGE, CREATE_PACKAGE, CATNIP_PACKAGE, FLYWHEEL_PACKAGE)
            .scanning(name -> true)
            .build();

    public static boolean admitted() {
        return GATE.admitted();
    }

    private ElectroEnergeticsMod() {
    }
}
