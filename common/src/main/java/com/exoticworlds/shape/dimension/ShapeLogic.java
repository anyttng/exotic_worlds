package com.exoticworlds.shape.dimension;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.DerivedDimension;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.LegacyDerivation;
import com.exoticworlds.core.world.ShapeModel;
import com.exoticworlds.shape.WorldLoopPresets;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

public interface ShapeLogic {
    ShapeModel spans(DimensionContext context);

    default boolean isOfferedIn(ResourceKey<LevelStem> dimension) {
        return true;
    }

    default boolean canTakePresets() {
        return false;
    }

    default ShapeOptions withPreset(WorldLoopPresets preset, ShapeOptions options) {
        return options;
    }

    default Optional<ShapeOptions> optionsFor(ShapeModel spans) {
        return Optional.empty();
    }

    default @Nullable FlatShape derive(FlatShape overworld, DerivedDimension dimension) {
        return LegacyDerivation.derive(overworld, dimension.overworldScale(), dimension.coordinateScale());
    }
}
