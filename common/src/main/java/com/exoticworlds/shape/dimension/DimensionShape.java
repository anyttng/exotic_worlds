package com.exoticworlds.shape.dimension;

import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.option.ShapeOptions;

import net.minecraft.resources.Identifier;

public interface DimensionShape {
    Identifier getId();

    FlatShape spans(DimensionContext context);

    boolean canTakePresets();

    ShapeOptions withPreset(int widthChunks, ShapeOptions options);

    ShapeOptions optionsFor(FlatShape spans);
}
