package com.exoticworlds.compat.astikorcarts;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;
import com.exoticworlds.compat.ModSymbol;

public class AstikorCartsMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    static final ModSymbol PULL_DELTA = new ModSymbol(
            "com/jusipat/astikorcartsredux/entity/AbstractDrawnEntity", "getRelativeTargetVec",
            "(F)Lnet/minecraft/world/phys/Vec3;");

    private static final ModPresence ASTIKOR_CARTS = ModPresence.of(LOGGER,
            "com/jusipat/astikorcartsredux/AstikorCartsRedux.class",
            "[astikor-compat] gate astikorcarts_present", PULL_DELTA);

    public AstikorCartsMixinPlugin() {
        super(ASTIKOR_CARTS);
    }
}
