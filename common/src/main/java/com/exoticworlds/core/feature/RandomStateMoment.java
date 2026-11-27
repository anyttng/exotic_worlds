package com.exoticworlds.core.feature;

import com.exoticworlds.core.fold.WorldFold;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.RandomState;

public record RandomStateMoment(RandomState randomState, WorldFold fold, ResourceKey<LevelStem> dimension,
        int seaLevel, NoiseOffsets offsets) {
    public void setNoiseOffset(GenerationFeature feature, double offset) {
        this.offsets.set(feature.getKey(), offset);
    }
}
