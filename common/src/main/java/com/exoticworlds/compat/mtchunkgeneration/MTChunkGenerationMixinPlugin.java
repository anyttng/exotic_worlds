package com.exoticworlds.compat.mtchunkgeneration;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.gate.ModPresence;
import com.exoticworlds.compat.gate.ModPresenceGatePlugin;

public class MTChunkGenerationMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MT_CHUNK_GENERATION_PACKAGE = "dev/theagameplayer/mtchunkgeneration/";

    private static final ModPresence MT_CHUNK_GENERATION = ModPresence.gate(LOGGER, "[ew/mtchunkgeneration-compat] gate mtchunkgeneration_present")
            .probing("dev/theagameplayer/mtchunkgeneration/world/level/levelgen/MTLayer.class")
            .checking("exotic_worlds.compat.mtchunkgeneration.mixins.json")
            .modPackages(MT_CHUNK_GENERATION_PACKAGE)
            .build();

    public MTChunkGenerationMixinPlugin() {
        super(MT_CHUNK_GENERATION);
    }
}
