package com.exoticworlds.shape.layout;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.core.NetherScales;
import com.exoticworlds.core.SpawnPoint;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

public record WorldLayout(DimensionLayout overworld, DimensionLayout nether, DimensionLayout end,
        Map<ResourceKey<LevelStem>, DimensionLayout> modDimensions, int netherScale) {
    public static final WorldLayout DEFAULT = new WorldLayout(
            DimensionLayout.normal(DimensionLayout.DEFAULT_OPTIONS),
            DimensionLayout.fromOverworld(DimensionLayout.DEFAULT_OPTIONS),
            DimensionLayout.fromOverworld(DimensionLayout.DEFAULT_OPTIONS),
            Map.of(), NetherScales.DEFAULT);

    public WorldLayout {
        modDimensions = Map.copyOf(modDimensions);
        if (overworld.kind() == ShapeKind.FROM_OVERWORLD) {
            throw new IllegalArgumentException("The Overworld cannot take its shape from itself");
        }

        if (modDimensions.values().stream().anyMatch(layout -> layout.kind() == ShapeKind.FROM_OVERWORLD)) {
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

    public @Nullable LoopSpans netherSpans() {
        return this.nether.kind() == ShapeKind.FROM_OVERWORLD
                ? OverworldDerivation.nether(this.overworld.spans(), this.netherScale)
                : this.nether.spans();
    }

    public @Nullable LoopSpans endSpans() {
        return this.end.kind() == ShapeKind.FROM_OVERWORLD
                ? OverworldDerivation.end(this.overworld.spans())
                : this.end.spans();
    }

    public @Nullable WorldLayout withSpawn(SpawnPoint spawn) {
        if (this.overworld.kind() == ShapeKind.NORMAL) {
            return null;
        }

        return new WorldLayout(this.overworld.withOptions(this.overworld.options().with(SpawnPoint.OPTION, spawn)),
                this.nether, this.end, this.modDimensions, this.netherScale);
    }
}
