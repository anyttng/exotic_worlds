package com.exoticworlds.shape.dimension;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.ShapeModel;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

public record DimensionContext(ResourceKey<LevelStem> dimension, ShapeOptions options, @Nullable ShapeModel overworld,
        double overworldScale, double coordinateScale) {
}
