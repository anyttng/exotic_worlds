package com.exoticworlds.compat.freeterraforged.banding;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;

public class FreeTerraForgedBandingMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ModPresence BANDING = ModPresence.gate(LOGGER,
                    "[freeterraforged-compat] gate freeterraforged_banding_present")
            .probing("etcodehome/freeterraforged/world/worldgen/biome/UndergroundBiomeBanding.class")
            .checking("exotic_worlds.compat.freeterraforged.banding.mixins.json")
            .build();

    public FreeTerraForgedBandingMixinPlugin() {
        super(BANDING);
    }
}
