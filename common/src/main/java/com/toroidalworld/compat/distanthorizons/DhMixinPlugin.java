package com.toroidalworld.compat.distanthorizons;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.toroidalworld.compat.ModPresence;
import com.toroidalworld.compat.ModPresenceGatePlugin;
import com.toroidalworld.compat.ModSymbol;

public class DhMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    static final ModSymbol LEVEL_CHUNK_HASH_REPO = new ModSymbol(
            "com/seibel/distanthorizons/core/level/AbstractDhLevel", "chunkHashRepo",
            "Lcom/seibel/distanthorizons/core/sql/repo/ChunkHashRepo;");

    static final ModSymbol REPO_UPSERT_STATEMENT = new ModSymbol(
            "com/seibel/distanthorizons/core/sql/repo/FullDataSourceV2Repo", "createUpsertStatement",
            "(Lcom/seibel/distanthorizons/core/sql/dto/FullDataSourceV2DTO;)Ljava/sql/PreparedStatement;");

    private static final ModPresence DH = ModPresence.of(LOGGER,
            "com/seibel/distanthorizons/core/api/internal/ClientApi.class",
            "[dh-compat] gate distanthorizons_present", LEVEL_CHUNK_HASH_REPO, REPO_UPSERT_STATEMENT);

    public DhMixinPlugin() {
        super(DH);
    }
}
