package com.exoticworlds.client.shape.layout;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.shape.layout.OverworldDerivation;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

public record CoordinateScales(Map<ResourceKey<LevelStem>, Double> byDimension) {
    private static final double UNKNOWN = 0.0;

    public CoordinateScales {
        byDimension = Map.copyOf(byDimension);
    }

    @Nullable LoopSpans modDimension(ResourceKey<LevelStem> dimension, @Nullable LoopSpans overworld) {
        return OverworldDerivation.modDimension(overworld, this.of(LevelStem.OVERWORLD), this.of(dimension));
    }

    private double of(ResourceKey<LevelStem> dimension) {
        return this.byDimension.getOrDefault(dimension, UNKNOWN);
    }
}
