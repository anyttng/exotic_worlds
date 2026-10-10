package com.exoticworlds.compat.ftbxaerocompat;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;

public class FtbXaeroMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ModPresence FTBXAERO = ModPresence.gate(LOGGER, "[ftbxaero-compat] gate ftbxaerocompat_present")
            .probing("dev/satherov/ftbxaerocompat/FTBXaeroCompat.class")
            .checking("exotic_worlds.compat.ftbxaerocompat.mixins.json")
            .build();

    public FtbXaeroMixinPlugin() {
        super(FTBXAERO);
    }
}
