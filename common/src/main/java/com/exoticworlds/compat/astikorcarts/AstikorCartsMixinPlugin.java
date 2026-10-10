package com.exoticworlds.compat.astikorcarts;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;

public class AstikorCartsMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ModPresence ASTIKOR_CARTS = ModPresence.gate(LOGGER, "[astikor-compat] gate astikorcarts_present")
            .probing("com/jusipat/astikorcartsredux/AstikorCartsRedux.class")
            .checking("exotic_worlds.compat.astikorcarts.mixins.json")
            .build();

    public AstikorCartsMixinPlugin() {
        super(ASTIKOR_CARTS);
    }
}
