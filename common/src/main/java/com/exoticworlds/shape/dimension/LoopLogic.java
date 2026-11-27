package com.exoticworlds.shape.dimension;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.DerivedDimension;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.WorldLoopBounds;
import com.exoticworlds.core.world.WorldLoopSizes;
import com.exoticworlds.shape.WorldLoopPresets;

import net.minecraft.core.Direction;

public abstract class LoopLogic implements ShapeLogic {

    @Override
    public final boolean canTakePresets() {
        return true;
    }

    @Override
    public final ShapeOptions withPreset(WorldLoopPresets preset, ShapeOptions options) {
        return this.withWidth(preset.getWidthChunks(), options);
    }

    protected abstract ShapeOptions withWidth(int widthChunks, ShapeOptions options);

    @Override
    public @Nullable FlatShape derive(FlatShape overworld, DerivedDimension dimension) {
        if (!dimension.isEndTerrain()) {
            return ShapeLogic.super.derive(overworld, dimension);
        }

        boolean isXLooped = overworld.bounds().isLooped(Direction.Axis.X);
        boolean isZLooped = overworld.bounds().isLooped(Direction.Axis.Z);
        if (isXLooped && isZLooped) {
            return FlatShape.unskewed(WorldLoopBounds.ofWidths(
                    endWidthChunks(overworld, Direction.Axis.X), endWidthChunks(overworld, Direction.Axis.Z)));
        }

        Direction.Axis looped = isXLooped ? Direction.Axis.X : Direction.Axis.Z;
        return FlatShape.unskewed(WorldLoopBounds.ofWidth(looped, endWidthChunks(overworld, looped)));
    }

    private static int endWidthChunks(FlatShape overworld, Direction.Axis axis) {
        return WorldLoopSizes.endWidthChunksFor(overworld.bounds().getWidthChunks(axis));
    }
}
