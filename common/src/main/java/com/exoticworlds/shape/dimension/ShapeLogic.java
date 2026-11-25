package com.exoticworlds.shape.dimension;

import java.util.Optional;

import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.shape.WorldLoopPresets;

public interface ShapeLogic {
    FlatShape spans(DimensionContext context);

    default boolean canTakePresets() {
        return false;
    }

    default ShapeOptions withPreset(WorldLoopPresets preset, ShapeOptions options) {
        return options;
    }

    default Optional<ShapeOptions> optionsFor(FlatShape spans) {
        return Optional.empty();
    }
}
