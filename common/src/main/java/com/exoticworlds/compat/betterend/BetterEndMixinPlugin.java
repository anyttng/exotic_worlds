package com.exoticworlds.compat.betterend;

import java.util.List;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.betterend.ambertation.AmbertationMixinPlugin;
import com.exoticworlds.compat.betterend.betterx.BetterxMixinPlugin;
import com.exoticworlds.compat.gate.ModPresence;
import com.exoticworlds.compat.gate.ModPresenceGatePlugin;

public class BetterEndMixinPlugin extends ModPresenceGatePlugin {
    public static final String GATE_LABEL = "[ew/betterend-compat] gate betterend";

    public static final String BETTER_END_PACKAGE = "org/betterx/betterend/";

    public static final String TERRAIN_GENERATOR_CLASS = "org/betterx/betterend/world/generator/TerrainGenerator.class";

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String BCLIB_PACKAGE = "org/betterx/bclib/";

    public static final ModPresence BETTER_END = ModPresence.gate(LOGGER, GATE_LABEL + "_present")
            .probing(TERRAIN_GENERATOR_CLASS)
            .checking("exotic_worlds.compat.betterend.mixins.json")
            .modPackages(BETTER_END_PACKAGE, BCLIB_PACKAGE)
            .requiringAny(List.of(() -> BetterxMixinPlugin.BETTER_END_BETTERX,
                    () -> AmbertationMixinPlugin.BETTER_END_AMBERTATION))
            .build();

    public BetterEndMixinPlugin() {
        super(BETTER_END);
    }
}
