package com.exoticworlds.shape.dimension;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.ShapeOptions;

import net.minecraft.resources.Identifier;

public interface DimensionShape {
    Identifier id();

    FlatShape spans(DimensionContext context);

    default boolean takesPresets() {
        return false;
    }

    default ShapeOptions withPreset(int chunkWidth, ShapeOptions options) {
        return options;
    }
}
