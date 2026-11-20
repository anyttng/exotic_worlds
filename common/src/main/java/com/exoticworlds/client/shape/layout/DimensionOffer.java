package com.exoticworlds.client.shape.layout;

import java.util.List;
import java.util.function.BiFunction;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.core.WorldLoopSizes;
import com.exoticworlds.shape.layout.OverworldDerivation;
import com.exoticworlds.shape.layout.ShapeKind;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

record DimensionOffer(ResourceKey<LevelStem> dimension, Component title, List<ShapeKind> kinds, boolean spawnPoint,
        boolean netherScale, int minChunkWidth, int defaultChunkWidth,
        @Nullable BiFunction<@Nullable LoopSpans, Integer, @Nullable LoopSpans> derivation) {
    private static final List<ShapeKind> OWN_KINDS = List.of(ShapeKind.NORMAL, ShapeKind.TORUS, ShapeKind.CYLINDER);
    private static final List<ShapeKind> DERIVED_KINDS =
            List.of(ShapeKind.FROM_OVERWORLD, ShapeKind.NORMAL, ShapeKind.TORUS, ShapeKind.CYLINDER);

    static final DimensionOffer OVERWORLD = new DimensionOffer(LevelStem.OVERWORLD, SettingsLines.OVERWORLD, OWN_KINDS,
            true, false,
            WorldLoopSizes.MIN_CHUNK_WIDTH, WorldLoopSizes.DEFAULT_CHUNK_WIDTH, null);

    static final DimensionOffer NETHER = new DimensionOffer(LevelStem.NETHER, SettingsLines.NETHER, DERIVED_KINDS,
            false, true,
            WorldLoopSizes.MIN_CHUNK_WIDTH, WorldLoopSizes.DEFAULT_CHUNK_WIDTH, OverworldDerivation::nether);

    static final DimensionOffer END = new DimensionOffer(LevelStem.END, SettingsLines.END, DERIVED_KINDS, false, false,
            WorldLoopSizes.END_MIN_CHUNK_WIDTH, WorldLoopSizes.END_DEFAULT_CHUNK_WIDTH,
            (overworld, netherScale) -> OverworldDerivation.end(overworld));

    static DimensionOffer modDimension(ResourceKey<LevelStem> dimension, CoordinateScales scales) {
        return new DimensionOffer(dimension, Component.literal(dimension.identifier().toString()), DERIVED_KINDS,
                false, false, WorldLoopSizes.MIN_CHUNK_WIDTH, WorldLoopSizes.DEFAULT_CHUNK_WIDTH,
                (overworld, netherScale) -> scales.modDimension(dimension, overworld));
    }
}
