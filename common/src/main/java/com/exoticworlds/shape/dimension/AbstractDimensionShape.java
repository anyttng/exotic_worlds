package com.exoticworlds.shape.dimension;

import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.option.ShapeOptions;

import net.minecraft.resources.Identifier;

public abstract class AbstractDimensionShape implements DimensionShape {
    private final Identifier id;

    protected AbstractDimensionShape(Identifier id) {
        this.id = id;
    }

    @Override
    public final Identifier getId() {
        return this.id;
    }

    @Override
    public boolean canTakePresets() {
        return false;
    }

    @Override
    public ShapeOptions withPreset(int widthChunks, ShapeOptions options) {
        return options;
    }

    @Override
    public ShapeOptions optionsFor(FlatShape spans) {
        return ShapeOptions.DEFAULT;
    }
}
