package com.exoticworlds.shape.dimension;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.ShapeOptions;

import net.minecraft.resources.Identifier;

public interface DimensionShape {
    Identifier id();

    FlatShape spans(DimensionContext context);

    boolean takesPresets();

    ShapeOptions withPreset(int chunkWidth, ShapeOptions options);

    ShapeOptions optionsFor(FlatShape spans);
}
