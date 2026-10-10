package com.exoticworlds.compat.electroenergetics;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;

public class ElectroEnergeticsMixinPlugin extends ModPresenceGatePlugin {
    static final String CONFIG = "exotic_worlds.compat.electroenergetics.mixins.json";

    private static final ModPresence SPAWN_GUARD = ModPresence.gate(LogUtils.getLogger(),
                    "[electroenergetics-compat] gate spawn_guard_present")
            .probing("com/george_vi/electroenergetics/foundation/device/SpawnPreventingDevice.class")
            .checking(CONFIG, ElectroEnergeticsMod.SPAWN_GUARD_MIXIN::equals)
            .build();

    public ElectroEnergeticsMixinPlugin() {
        super(ElectroEnergeticsMod.GATE, SPAWN_GUARD);
    }
}
