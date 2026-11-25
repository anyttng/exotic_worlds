package com.exoticworlds.client.shape.layout;

import java.util.List;
import java.util.function.BiFunction;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.shape.layout.OverworldDerivation;
import com.exoticworlds.shape.layout.ShapeKind;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

record DimensionOffer(ResourceKey<LevelStem> dimension, Component title, List<ShapeKind> kinds, boolean hasNetherScale,
        @Nullable BiFunction<@Nullable FlatShape, Integer, @Nullable FlatShape> derivation) {
    private static final List<ShapeKind> OWN_KINDS = List.of(ShapeKind.NORMAL);
    private static final List<ShapeKind> DERIVED_KINDS = List.of(ShapeKind.FROM_OVERWORLD, ShapeKind.NORMAL);

    static final DimensionOffer OVERWORLD = new DimensionOffer(LevelStem.OVERWORLD, SettingsLines.OVERWORLD, OWN_KINDS,
            false, null);

    static final DimensionOffer NETHER = new DimensionOffer(LevelStem.NETHER, SettingsLines.NETHER, DERIVED_KINDS,
            true, OverworldDerivation::nether);

    static final DimensionOffer END = new DimensionOffer(LevelStem.END, SettingsLines.END, DERIVED_KINDS, false,
            (overworld, netherScale) -> OverworldDerivation.end(overworld));

    static DimensionOffer modDimension(ResourceKey<LevelStem> dimension, CoordinateScales scales) {
        return new DimensionOffer(dimension, Component.literal(dimension.identifier().toString()), DERIVED_KINDS,
                false, (overworld, netherScale) -> scales.modDimension(dimension, overworld));
    }
}
