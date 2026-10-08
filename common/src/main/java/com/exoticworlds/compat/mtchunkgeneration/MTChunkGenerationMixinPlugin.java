package com.exoticworlds.compat.mtchunkgeneration;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;
import com.exoticworlds.compat.ModSymbol;

public class MTChunkGenerationMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String MT_LAYER = "dev/theagameplayer/mtchunkgeneration/world/level/levelgen/MTLayer";

    private static final ModSymbol DO_FILL = new ModSymbol(MT_LAYER, "doFill",
            "(Lnet/minecraft/world/level/levelgen/blending/Blender;Lnet/minecraft/world/level/StructureManager;"
                    + "Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/chunk/ChunkAccess;II)"
                    + "Lnet/minecraft/world/level/chunk/ChunkAccess;");

    private static final ModSymbol DO_CREATE_BIOMES = new ModSymbol(MT_LAYER, "doCreateBiomes",
            "(Lnet/minecraft/world/level/levelgen/blending/Blender;Lnet/minecraft/world/level/levelgen/RandomState;"
                    + "Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/chunk/ChunkAccess;)V");

    private static final ModSymbol GENERATOR = new ModSymbol(MT_LAYER, "generator",
            "Lnet/minecraft/world/level/levelgen/NoiseBasedChunkGenerator;");

    private static final ModPresence MT_CHUNK_GENERATION = ModPresence.of(LOGGER, MT_LAYER + ".class",
            "[mtchunkgeneration-compat] gate mtchunkgeneration_present", DO_FILL, DO_CREATE_BIOMES, GENERATOR);

    public MTChunkGenerationMixinPlugin() {
        super(MT_CHUNK_GENERATION);
    }
}
