package com.exoticworlds.compat.adlods;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;

public class AdLodsMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String ADLODS_PACKAGE = "com/endertech/minecraft/mods/adlods/";

    private static final ModPresence ADLODS = ModPresence
            .gate(LOGGER, "[ew/adlods-compat] gate adlods_present")
            .probing("com/endertech/minecraft/mods/adlods/world/WorldTargets.class")
            .checking("exotic_worlds.compat.adlods.mixins.json")
            .modPackages(ADLODS_PACKAGE)
            .build();

    public AdLodsMixinPlugin() {
        super(ADLODS);
    }
}
