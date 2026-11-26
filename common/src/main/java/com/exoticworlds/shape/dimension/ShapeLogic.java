package com.exoticworlds.shape.dimension;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.DerivedDimension;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.LegacyDerivation;
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

    default @Nullable FlatShape derive(FlatShape overworld, DerivedDimension dimension) {
        return LegacyDerivation.derive(overworld, dimension.overworldScale(), dimension.coordinateScale());
    }
}
