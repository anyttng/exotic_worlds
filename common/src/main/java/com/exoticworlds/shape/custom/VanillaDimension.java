package com.exoticworlds.shape.custom;

import com.exoticworlds.core.WorldLoopSizes;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

public enum VanillaDimension implements CustomStem {
    OVERWORLD(WorldLoopSizes.DEFAULT_CHUNK_WIDTH),
    NETHER(WorldLoopSizes.DEFAULT_CHUNK_WIDTH),
    END(WorldLoopSizes.END_DEFAULT_CHUNK_WIDTH);

    private final int defaultChunkWidth;

    VanillaDimension(int defaultChunkWidth) {
        this.defaultChunkWidth = defaultChunkWidth;
    }

    @Override
    public ResourceKey<LevelStem> stem() {
        return switch (this) {
            case OVERWORLD -> LevelStem.OVERWORLD;
            case NETHER -> LevelStem.NETHER;
            case END -> LevelStem.END;
        };
    }

    @Override
    public int defaultChunkWidth() {
        return this.defaultChunkWidth;
    }
}
