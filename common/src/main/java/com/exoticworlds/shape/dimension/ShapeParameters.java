package com.exoticworlds.shape.dimension;

import java.util.List;

import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.shape.option.DimensionOption;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

public interface ShapeParameters {
    List<DimensionOption<?>> getAll();

    ShapeOptions defaultOptions(ResourceKey<LevelStem> dimension);
}
