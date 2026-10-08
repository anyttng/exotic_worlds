package com.exoticworlds.compat.biolith;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;
import com.exoticworlds.compat.ModSymbol;

public class BiolithMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String LOCAL_NOISE_NAME = "getLocalNoise";

    private static final String LOCAL_NOISE_DESCRIPTOR = "(III)D";

    static final ModSymbol OVERWORLD_LOCAL_NOISE = new ModSymbol(
            "com/terraformersmc/biolith/impl/biome/OverworldBiomePlacement", LOCAL_NOISE_NAME,
            LOCAL_NOISE_DESCRIPTOR);

    static final ModSymbol NETHER_LOCAL_NOISE = new ModSymbol(
            "com/terraformersmc/biolith/impl/biome/NetherBiomePlacement", LOCAL_NOISE_NAME, LOCAL_NOISE_DESCRIPTOR);

    private static final ModPresence BIOLITH = ModPresence.of(LOGGER,
            "com/terraformersmc/biolith/impl/biome/OverworldBiomePlacement.class",
            "[biolith-compat] gate biolith_present", OVERWORLD_LOCAL_NOISE, NETHER_LOCAL_NOISE);

    public BiolithMixinPlugin() {
        super(BIOLITH);
    }
}
