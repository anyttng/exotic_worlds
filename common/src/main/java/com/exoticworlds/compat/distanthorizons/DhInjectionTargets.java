package com.exoticworlds.compat.distanthorizons;

public final class DhInjectionTargets {
    // The one jar carries both loaders' chunk wrapper; its NeoForge build is renamed apart rather than shared.
    public static final String CHUNK_WRAPPER_NEOFORGE =
            "com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper_neoforge";
    public static final String CHUNK_WRAPPER = "com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper";

    private DhInjectionTargets() {
    }
}
