package com.exoticworlds.shape.dimension;

import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.shape.WorldLoopPresets;

public abstract class LoopLogic implements ShapeLogic {

    @Override
    public final boolean canTakePresets() {
        return true;
    }

    @Override
    public final ShapeOptions withPreset(WorldLoopPresets preset, ShapeOptions options) {
        return this.withWidth(preset.getWidthChunks(), options);
    }

    protected abstract ShapeOptions withWidth(int widthChunks, ShapeOptions options);
}
