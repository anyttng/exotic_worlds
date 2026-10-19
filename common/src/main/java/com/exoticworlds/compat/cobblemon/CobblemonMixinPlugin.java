package com.exoticworlds.compat.cobblemon;

import org.slf4j.Logger;

import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;
import com.mojang.logging.LogUtils;

public class CobblemonMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String CONFIG = "exotic_worlds.compat.cobblemon.mixins.json";
    private static final String CAN_SEE_MIXIN = "CanSeeAimMixin";

    private static final ModPresence COBBLEMON = ModPresence.gate(LOGGER, "[ew/cobblemon-compat] gate cobblemon_present")
            .probing("com/cobblemon/mod/common/util/PlayerExtensionsKt.class")
            .checking(CONFIG, mixin -> !mixin.equals(CAN_SEE_MIXIN))
            .bodyFrom("EnderPearlLookMixin", "com/cobblemon/mod/common/mixin/EnderPearlItemMixin")
            .build();

    private static final ModPresence CAN_SEE = ModPresence.gate(LOGGER, "[ew/cobblemon-compat] gate can_see_present")
            .probing("com/cobblemon/mod/common/api/molang/function/LivingEntityMoLangFunctions.class")
            .checking(CONFIG, CAN_SEE_MIXIN::equals)
            .build();

    public CobblemonMixinPlugin() {
        super(COBBLEMON, CAN_SEE);
    }
}
