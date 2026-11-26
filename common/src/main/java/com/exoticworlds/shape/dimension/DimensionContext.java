package com.exoticworlds.shape.dimension;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.option.ShapeOptions;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

public record DimensionContext(ResourceKey<LevelStem> dimension, ShapeOptions options, @Nullable FlatShape overworld,
        double overworldScale, double coordinateScale) {
}
