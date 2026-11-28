package com.exoticworlds.compat.betterend.ambertation;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.betterend.BetterEndMixinPlugin;
import com.exoticworlds.compat.gate.ModPresence;
import com.exoticworlds.compat.gate.ModPresenceGatePlugin;

public class AmbertationMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String WOVER_PACKAGE = "de/ambertation/wover/";

    public static final ModPresence BETTER_END_AMBERTATION =
            ModPresence.gate(LOGGER, BetterEndMixinPlugin.GATE_LABEL + "_ambertation_present")
                    .probing(BetterEndMixinPlugin.TERRAIN_GENERATOR_CLASS,
                            "de/ambertation/wover/generator/api/biomesource/end/WoverEndConfig.class")
                    .checking("exotic_worlds.compat.betterend.ambertation.mixins.json")
                    .modPackages(BetterEndMixinPlugin.BETTER_END_PACKAGE, WOVER_PACKAGE)
                    .requiring(BetterEndMixinPlugin.BETTER_END)
                    .build();

    public AmbertationMixinPlugin() {
        super(BETTER_END_AMBERTATION);
    }
}
