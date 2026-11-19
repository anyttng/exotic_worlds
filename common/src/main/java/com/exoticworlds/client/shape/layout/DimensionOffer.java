package com.exoticworlds.client.shape.layout;

import java.util.List;
import java.util.function.BiFunction;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.option.WorldOption;
import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.core.WorldLoopSizes;
import com.exoticworlds.shape.layout.OverworldDerivation;
import com.exoticworlds.shape.layout.ShapeKind;
import com.exoticworlds.shape.torus.GuaranteedLand;
import com.exoticworlds.shape.torus.GuaranteedNetherComplexes;

import net.minecraft.network.chat.Component;

record DimensionOffer(Component title, List<ShapeKind> kinds, boolean spawnPoint, boolean netherScale,
        @Nullable TorusToggle torusToggle, int minChunkWidth, int defaultChunkWidth,
        @Nullable BiFunction<@Nullable LoopSpans, Integer, @Nullable LoopSpans> derivation) {
    private static final List<ShapeKind> OWN_KINDS = List.of(ShapeKind.NORMAL, ShapeKind.TORUS, ShapeKind.CYLINDER);
    private static final List<ShapeKind> DERIVED_KINDS =
            List.of(ShapeKind.FROM_OVERWORLD, ShapeKind.NORMAL, ShapeKind.TORUS, ShapeKind.CYLINDER);

    static final DimensionOffer OVERWORLD = new DimensionOffer(SettingsLines.OVERWORLD, OWN_KINDS, true, false,
            new TorusToggle(SettingsLines.GUARANTEED_LAND_KEY, GuaranteedLand.OPTION),
            WorldLoopSizes.MIN_CHUNK_WIDTH, WorldLoopSizes.DEFAULT_CHUNK_WIDTH, null);

    static final DimensionOffer NETHER = new DimensionOffer(SettingsLines.NETHER, DERIVED_KINDS, false, true,
            new TorusToggle(SettingsLines.GUARANTEED_NETHER_COMPLEXES_KEY, GuaranteedNetherComplexes.OPTION),
            WorldLoopSizes.MIN_CHUNK_WIDTH, WorldLoopSizes.DEFAULT_CHUNK_WIDTH, OverworldDerivation::nether);

    static final DimensionOffer END = new DimensionOffer(SettingsLines.END, DERIVED_KINDS, false, false, null,
            WorldLoopSizes.END_MIN_CHUNK_WIDTH, WorldLoopSizes.END_DEFAULT_CHUNK_WIDTH,
            (overworld, netherScale) -> OverworldDerivation.end(overworld));

    static DimensionOffer modDimension(ModStem stem) {
        return new DimensionOffer(Component.literal(stem.key().identifier().toString()), DERIVED_KINDS, false, false,
                null, WorldLoopSizes.MIN_CHUNK_WIDTH, WorldLoopSizes.DEFAULT_CHUNK_WIDTH,
                (overworld, netherScale) -> stem.derived(overworld));
    }

    record TorusToggle(String captionKey, WorldOption<Boolean> option) {
    }
}
