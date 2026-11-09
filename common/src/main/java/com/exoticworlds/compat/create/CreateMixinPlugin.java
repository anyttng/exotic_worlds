package com.exoticworlds.compat.create;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;

public class CreateMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String CREATE_PACKAGE = "com/zurrtum/create/";

    static final ModPresence CREATE = ModPresence.gate(LOGGER, "[ew/create-compat] gate create_present")
            .probing("com/zurrtum/create/Create.class")
            .checking("exotic_worlds.compat.create.mixins.json")
            .modPackages(CREATE_PACKAGE)
            .build();

    public CreateMixinPlugin() {
        super(CREATE);
    }
}
