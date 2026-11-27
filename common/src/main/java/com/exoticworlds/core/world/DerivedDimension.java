package com.exoticworlds.core.world;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

public record DerivedDimension(ResourceKey<LevelStem> dimension, boolean isEndTerrain, double overworldScale,
        double coordinateScale) {
    public static DerivedDimension ofKey(ResourceKey<LevelStem> dimension, double overworldScale,
            double coordinateScale) {
        return new DerivedDimension(dimension, StemTerrain.ofKey(dimension).isEndTerrain(), overworldScale,
                coordinateScale);
    }
}
