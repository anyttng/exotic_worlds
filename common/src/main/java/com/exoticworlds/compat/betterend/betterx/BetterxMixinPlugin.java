package com.exoticworlds.compat.betterend.betterx;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.betterend.BetterEndMixinPlugin;
import com.exoticworlds.compat.gate.ModPresence;
import com.exoticworlds.compat.gate.ModPresenceGatePlugin;

public class BetterxMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String BETTERX_PACKAGE = "org/betterx/";

    public static final ModPresence BETTER_END_BETTERX =
            ModPresence.gate(LOGGER, BetterEndMixinPlugin.GATE_LABEL + "_betterx_present")
                    .probing(BetterEndMixinPlugin.TERRAIN_GENERATOR_CLASS,
                            "org/betterx/wover/generator/api/biomesource/end/WoverEndConfig.class")
                    .checking("exotic_worlds.compat.betterend.betterx.mixins.json")
                    .modPackages(BETTERX_PACKAGE)
                    .requiring(BetterEndMixinPlugin.BETTER_END)
                    .build();

    public BetterxMixinPlugin() {
        super(BETTER_END_BETTERX);
    }
}
