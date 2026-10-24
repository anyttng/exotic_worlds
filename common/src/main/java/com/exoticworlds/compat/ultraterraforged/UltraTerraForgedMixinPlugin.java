package com.exoticworlds.compat.ultraterraforged;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;

public class UltraTerraForgedMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ModPresence ULTRATERRAFORGED = ModPresence.gate(LOGGER,
                    "[ew/ultraterraforged-compat] gate ultraterraforged_present")
            .probing("com/pandaismyname1/ultraterraforged/world/worldgen/heightmap/Heightmap.class")
            .checking("exotic_worlds.compat.ultraterraforged.mixins.json")
            .build();

    public UltraTerraForgedMixinPlugin() {
        super(ULTRATERRAFORGED);
    }
}
