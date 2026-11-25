package com.exoticworlds.shape.dimension;

import java.util.List;
import java.util.Optional;

import com.exoticworlds.core.world.FlatShape;
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

    FlatShape spans(DimensionContext context);

    List<DimensionOption<?>> getParameters();

    ShapeOptions defaultOptions(ResourceKey<LevelStem> dimension);

    boolean canTakePresets();

    ShapeOptions withPreset(WorldLoopPresets preset, ShapeOptions options);

    Optional<ShapeOptions> optionsFor(FlatShape spans);
}
