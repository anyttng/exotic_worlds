package com.exoticworlds.client.shape.layout;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.shape.layout.DimensionLayout;
import com.exoticworlds.shape.layout.OverworldDerivation;
import com.exoticworlds.shape.layout.ShapeKind;
import com.exoticworlds.shape.layout.WorldLayout;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

public record ModStem(ResourceKey<LevelStem> key, double overworldScale, double coordinateScale) {

    @Nullable LoopSpans derived(@Nullable LoopSpans overworld) {
        return OverworldDerivation.modDimension(overworld, this.overworldScale, this.coordinateScale);
    }

    @Nullable LoopSpans spans(WorldLayout layout) {
        DimensionLayout chosen = layout.modDimension(this.key);
        return chosen.kind() == ShapeKind.FROM_OVERWORLD ? this.derived(layout.overworld().spans()) : chosen.spans();
    }
}
