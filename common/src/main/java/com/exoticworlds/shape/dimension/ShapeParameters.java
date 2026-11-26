package com.exoticworlds.shape.dimension;

import java.util.List;

import com.exoticworlds.core.world.StemTerrain;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.shape.option.DimensionOption;

public interface ShapeParameters {
    List<DimensionOption<?>> getAll();

    ShapeOptions defaultOptions(StemTerrain stem);
}
