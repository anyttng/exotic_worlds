package com.exoticworlds.compat.ftbchunks;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;
import com.exoticworlds.compat.ModSymbol;

public class FtbChunksMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    static final ModSymbol OWNING_TEAM = new ModSymbol("dev/ftb/mods/ftbchunks/client/map/MapDimension",
            "getOwningTeam", "(Lnet/minecraft/world/level/ChunkPos;)Ldev/ftb/mods/ftbteams/api/Team;");

    static final ModSymbol MINIMAP_SPLITS = new ModSymbol(
            "dev/ftb/mods/ftbchunks/client/minimap/MinimapRegionCutoutTexture", "regionSplits", "(I)[I");

    private static final ModPresence FTBCHUNKS = ModPresence.of(LOGGER, "dev/ftb/mods/ftbchunks/FTBChunks.class",
            "[ftbc-compat] gate ftbchunks_present", OWNING_TEAM, MINIMAP_SPLITS);

    public FtbChunksMixinPlugin() {
        super(FTBCHUNKS);
    }
}
