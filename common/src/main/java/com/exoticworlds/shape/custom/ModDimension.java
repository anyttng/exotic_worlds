package com.exoticworlds.shape.custom;

import com.exoticworlds.core.WorldLoopSizes;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

public record ModDimension(ResourceKey<LevelStem> stem) implements CustomStem {
    @Override
    public int defaultChunkWidth() {
        return WorldLoopSizes.DEFAULT_CHUNK_WIDTH;
    }
}
