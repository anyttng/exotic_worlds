package com.exoticworlds.core.world;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

public record StemTerrain(ResourceKey<LevelStem> stem, boolean isEndTerrain) {
    public static StemTerrain ofKey(ResourceKey<LevelStem> stem) {
        return new StemTerrain(stem, stem == LevelStem.END);
    }
}
