package com.exoticworlds.shape.dimension;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.ShapeOptions;

import net.minecraft.resources.Identifier;

public abstract class AbstractDimensionShape implements DimensionShape {
    private final Identifier id;

    protected AbstractDimensionShape(Identifier id) {
        this.id = id;
    }

    @Override
    public final Identifier id() {
        return this.id;
    }

    @Override
    public boolean takesPresets() {
        return false;
    }

    @Override
    public ShapeOptions withPreset(int chunkWidth, ShapeOptions options) {
        return options;
    }

    @Override
    public ShapeOptions optionsFor(FlatShape spans) {
        return ShapeOptions.DEFAULT;
    }
}
