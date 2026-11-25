package com.exoticworlds.shape.layout.client;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.shape.layout.OverworldDerivation;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

public record CoordinateScales(Map<ResourceKey<LevelStem>, Double> byDimension) {
    private static final double UNKNOWN = 0.0;

    public CoordinateScales {
        byDimension = Map.copyOf(byDimension);
    }

    @Nullable FlatShape modDimension(ResourceKey<LevelStem> dimension, @Nullable FlatShape overworld) {
        return OverworldDerivation.modDimension(overworld, this.of(LevelStem.OVERWORLD), this.of(dimension));
    }

    public double of(ResourceKey<LevelStem> dimension) {
        return this.byDimension.getOrDefault(dimension, UNKNOWN);
    }
}
