package com.exoticworlds.shape.dimension;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.LegacyDerivation;
import com.exoticworlds.shape.WorldLoopPresets;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

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

    default @Nullable FlatShape derive(FlatShape overworld, ResourceKey<LevelStem> dimension, double overworldScale,
            double coordinateScale) {
        return LegacyDerivation.derive(overworld, overworldScale, coordinateScale);
    }
}
