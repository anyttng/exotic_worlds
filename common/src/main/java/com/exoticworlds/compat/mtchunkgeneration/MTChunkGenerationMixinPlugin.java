package com.exoticworlds.compat.mtchunkgeneration;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;

public class MTChunkGenerationMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ModPresence MT_CHUNK_GENERATION = ModPresence.gate(LOGGER, "[mtchunkgeneration-compat] gate mtchunkgeneration_present")
            .probing("dev/theagameplayer/mtchunkgeneration/world/level/levelgen/MTLayer.class")
            .checking("exotic_worlds.compat.mtchunkgeneration.mixins.json")
            .build();

    public MTChunkGenerationMixinPlugin() {
        super(MT_CHUNK_GENERATION);
    }
}
