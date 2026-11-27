package com.exoticworlds.shape.layout;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.world.DerivedDimension;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.NetherScales;
import com.exoticworlds.core.world.ShapeDerivations;
import com.exoticworlds.core.world.ShapeModel;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.feature.ShapeFeatures;
import com.exoticworlds.shape.feature.OptionFeature;
import com.exoticworlds.shape.feature.OptionFeatures;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

public record WorldLayout(DimensionLayout overworld, DimensionLayout nether, DimensionLayout end,
        Map<ResourceKey<LevelStem>, DimensionLayout> modDimensions, int netherScale) {
    private static final double UNSCALED = 1.0;

    public static WorldLayout newWorld() {
        ShapeOptions options = OptionFeatures.newWorldOptions();
        return new WorldLayout(DimensionLayout.normal(options), DimensionLayout.fromOverworld(options),
                DimensionLayout.fromOverworld(options), Map.of(), NetherScales.DEFAULT);
    }

    public WorldLayout {
        modDimensions = Map.copyOf(modDimensions);
        if (overworld.mode() == DimensionMode.FROM_OVERWORLD) {
            throw new IllegalArgumentException("The Overworld cannot take its shape from itself");
        }

        if (modDimensions.values().stream().anyMatch(layout -> layout.mode() == DimensionMode.FROM_OVERWORLD)) {
            throw new IllegalArgumentException("A mod dimension from the Overworld is one with no row");
        }
    }

    public WorldLayout withOverworld(DimensionLayout chosen) {
        return new WorldLayout(chosen, this.nether, this.end, this.modDimensions, this.netherScale);
    }

    public WorldLayout withNether(DimensionLayout chosen, int scale) {
        return new WorldLayout(this.overworld, chosen, this.end, this.modDimensions, scale);
    }

    public WorldLayout withEnd(DimensionLayout chosen) {
        return new WorldLayout(this.overworld, this.nether, chosen, this.modDimensions, this.netherScale);
    }

    public DimensionLayout getModDimension(ResourceKey<LevelStem> key) {
        DimensionLayout row = this.modDimensions.get(key);
        return row != null ? row : DimensionLayout.fromOverworld(OptionFeatures.newWorldOptions());
    }

    public WorldLayout withModDimension(ResourceKey<LevelStem> key, DimensionLayout chosen) {
        Map<ResourceKey<LevelStem>, DimensionLayout> rows = new HashMap<>(this.modDimensions);
        if (chosen.mode() == DimensionMode.FROM_OVERWORLD) {
            rows.remove(key);
        } else {
            rows.put(key, chosen);
        }

        return new WorldLayout(this.overworld, this.nether, this.end, rows, this.netherScale);
    }

    public @Nullable ShapeModel getNetherSpans() {
        return this.nether.mode() == DimensionMode.FROM_OVERWORLD
                ? derivedNether(this.overworld, this.netherScale)
                : this.nether.spans();
    }

    public @Nullable ShapeModel getEndSpans() {
        return this.end.mode() == DimensionMode.FROM_OVERWORLD ? derivedEnd(this.overworld) : this.end.spans();
    }

    public static @Nullable FlatShape derivedNether(DimensionLayout overworld, int netherScale) {
        return !(overworld.spans() instanceof FlatShape spans)
                ? null
                : ShapeDerivations.derive(overworld.shape(), spans, DerivedDimension.ofKey(LevelStem.NETHER,
                        UNSCALED, NetherScales.normalize(netherScale, NetherScales.allowedFor(spans))));
    }

    public static @Nullable FlatShape derivedEnd(DimensionLayout overworld) {
        return ShapeDerivations.derive(overworld.shape(), overworld.spans(),
                DerivedDimension.ofKey(LevelStem.END, UNSCALED, UNSCALED));
    }

    public <T> @Nullable WorldLayout withOverworldValue(OptionFeature<T> feature, T value) {
        Identifier shape = this.overworld.shape();
        if (shape == null || this.overworld.spans() == null
                || !ShapeFeatures.isAttached(shape, feature, LevelStem.OVERWORLD)) {
            return null;
        }

        return new WorldLayout(this.overworld.withOptions(this.overworld.options().with(feature.getOption(), value)),
                this.nether, this.end, this.modDimensions, this.netherScale);
    }
}
