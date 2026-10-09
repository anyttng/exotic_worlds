package com.exoticworlds.compat.ftbchunks;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;

public class FtbChunksMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ModPresence FTBCHUNKS = ModPresence.gate(LOGGER, "[ftbc-compat] gate ftbchunks_present")
            .probing("dev/ftb/mods/ftbchunks/FTBChunks.class")
            .checking("exotic_worlds.compat.ftbchunks.mixins.json")
            .build();

    public FtbChunksMixinPlugin() {
        super(FTBCHUNKS);
    }
}
