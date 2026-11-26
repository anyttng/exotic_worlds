package com.exoticworlds.shape.layout.client;

import java.util.List;
import java.util.function.BiFunction;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.shape.layout.DimensionLayout;
import com.exoticworlds.shape.layout.DimensionMode;
import com.exoticworlds.shape.layout.WorldLayout;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

record DimensionOffer(ResourceKey<LevelStem> dimension, Component title, List<DimensionMode> modes, boolean hasNetherScale,
        @Nullable BiFunction<DimensionLayout, Integer, @Nullable FlatShape> derivation) {
    private static final List<DimensionMode> OWN_MODES = List.of(DimensionMode.NORMAL);
    private static final List<DimensionMode> DERIVED_MODES = List.of(DimensionMode.FROM_OVERWORLD, DimensionMode.NORMAL);

    static final DimensionOffer OVERWORLD = new DimensionOffer(LevelStem.OVERWORLD, SettingsLines.OVERWORLD, OWN_MODES,
            false, null);

    static final DimensionOffer NETHER = new DimensionOffer(LevelStem.NETHER, SettingsLines.NETHER, DERIVED_MODES,
            true, WorldLayout::derivedNether);

    static final DimensionOffer END = new DimensionOffer(LevelStem.END, SettingsLines.END, DERIVED_MODES, false,
            (overworld, netherScale) -> WorldLayout.derivedEnd(overworld));

    static DimensionOffer modDimension(ResourceKey<LevelStem> dimension, StemFacts facts) {
        return new DimensionOffer(dimension, Component.literal(dimension.identifier().toString()), DERIVED_MODES,
                false, (overworld, netherScale) -> facts.modDimension(dimension, overworld));
    }
}
