package com.exoticworlds.shape.dimension;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.world.StemTerrain;
import com.exoticworlds.core.world.DerivedDimension;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.LegacyDerivation;
import com.exoticworlds.core.world.ShapeModel;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.shape.WorldLoopPresets;
import com.exoticworlds.shape.option.DimensionOption;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

public interface DimensionShape {
    Identifier getId();

    Component getName();

    Component getHint();

    Component describe(ShapeOptions options);

    ShapeModel spans(DimensionContext context);

    default boolean isOfferedIn(ResourceKey<LevelStem> dimension) {
        return true;
    }

    List<DimensionOption<?>> getParameters();

    ShapeOptions defaultOptions(StemTerrain stem);

    boolean canTakePresets();

    ShapeOptions withPreset(WorldLoopPresets preset, ShapeOptions options);

    Optional<ShapeOptions> optionsFor(ShapeModel spans);

    default @Nullable FlatShape derive(FlatShape overworld, DerivedDimension dimension) {
        return LegacyDerivation.derive(overworld, dimension.overworldScale(), dimension.coordinateScale());
    }
}
