package com.exoticworlds.client.shape.layout;

import java.util.List;
import java.util.function.BiFunction;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.WorldLoopSizes;
import com.exoticworlds.shape.layout.OverworldDerivation;
import com.exoticworlds.shape.layout.ShapeKind;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

record DimensionOffer(ResourceKey<LevelStem> dimension, Component title, List<ShapeKind> kinds, boolean hasSpawnPoint,
        boolean hasNetherScale, int minWidthChunks, int defaultWidthChunks,
        @Nullable BiFunction<@Nullable FlatShape, Integer, @Nullable FlatShape> derivation) {
    private static final List<ShapeKind> OWN_KINDS = List.of(ShapeKind.NORMAL, ShapeKind.TORUS, ShapeKind.CYLINDER);
    private static final List<ShapeKind> DERIVED_KINDS =
            List.of(ShapeKind.FROM_OVERWORLD, ShapeKind.NORMAL, ShapeKind.TORUS, ShapeKind.CYLINDER);

    static final DimensionOffer OVERWORLD = new DimensionOffer(LevelStem.OVERWORLD, SettingsLines.OVERWORLD, OWN_KINDS,
            true, false,
            WorldLoopSizes.MIN_WIDTH_CHUNKS, WorldLoopSizes.DEFAULT_WIDTH_CHUNKS, null);

    static final DimensionOffer NETHER = new DimensionOffer(LevelStem.NETHER, SettingsLines.NETHER, DERIVED_KINDS,
            false, true,
            WorldLoopSizes.MIN_WIDTH_CHUNKS, WorldLoopSizes.DEFAULT_WIDTH_CHUNKS, OverworldDerivation::nether);

    static final DimensionOffer END = new DimensionOffer(LevelStem.END, SettingsLines.END, DERIVED_KINDS, false, false,
            WorldLoopSizes.END_MIN_WIDTH_CHUNKS, WorldLoopSizes.END_DEFAULT_WIDTH_CHUNKS,
            (overworld, netherScale) -> OverworldDerivation.end(overworld));

    static DimensionOffer modDimension(ResourceKey<LevelStem> dimension, CoordinateScales scales) {
        return new DimensionOffer(dimension, Component.literal(dimension.identifier().toString()), DERIVED_KINDS,
                false, false, WorldLoopSizes.MIN_WIDTH_CHUNKS, WorldLoopSizes.DEFAULT_WIDTH_CHUNKS,
                (overworld, netherScale) -> scales.modDimension(dimension, overworld));
    }
}
