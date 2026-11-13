package com.exoticworlds.compat.freeterraforged.spawn;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.gate.ModPresence;
import com.exoticworlds.compat.gate.ModPresenceGatePlugin;

public class FreeTerraForgedSpawnMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FREETERRAFORGED_PACKAGE = "etcodehome/freeterraforged/";

    private static final ModPresence SPAWN = ModPresence.gate(LOGGER,
                    "[ew/freeterraforged-compat] gate freeterraforged_spawn_present")
            .probing("etcodehome/freeterraforged/mixin/MixinSpawnFinder.class")
            .checking("exotic_worlds.compat.freeterraforged.spawn.mixins.json")
            .bodyFrom("SpawnFinderUserSelectedMixin", FREETERRAFORGED_PACKAGE + "mixin/MixinSpawnFinder")
            .modPackages(FREETERRAFORGED_PACKAGE)
            .build();

    public FreeTerraForgedSpawnMixinPlugin() {
        super(SPAWN);
    }
}
