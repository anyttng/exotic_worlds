package com.exoticworlds.compat.electroenergetics;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;

public final class ElectroEnergeticsMod {
    static final String SPAWN_GUARD_MIXIN = "BulbSpawnGuardMixin";

    static final ModPresence GATE = ModPresence.gate(LogUtils.getLogger(),
                    "[electroenergetics-compat] gate electroenergetics_present")
            .probing("com/george_vi/electroenergetics/CreateElectroEnergetics.class")
            .checking(ElectroEnergeticsMixinPlugin.CONFIG, mixin -> !mixin.equals(SPAWN_GUARD_MIXIN))
            .build();

    public static boolean present() {
        return GATE.present();
    }

    private ElectroEnergeticsMod() {
    }
}
