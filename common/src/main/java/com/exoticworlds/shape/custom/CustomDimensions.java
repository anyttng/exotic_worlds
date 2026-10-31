package com.exoticworlds.shape.custom;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.api.v1.shape.ShapeDimensions;
import com.exoticworlds.core.CarriedShape;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.engine.gen.ShapedDimensions;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;

public final class CustomDimensions {

    public static WorldDimensions apply(WorldDimensions dimensions, CustomSettings settings) {
        WorldDimensions shaped = dimensions;
        for (VanillaDimension dimension : VanillaDimension.values()) {
            shaped = withShape(shaped, dimension.stem(), settings.get(dimension));
        }

        return shaped;
    }

    public static @Nullable CustomSettings read(WorldDimensions dimensions) {
        DimensionShape overworld = readShape(dimensions, LevelStem.OVERWORLD);
        DimensionShape nether = readShape(dimensions, LevelStem.NETHER);
        DimensionShape end = readShape(dimensions, LevelStem.END);
        if (overworld == null || nether == null || end == null) {
            return null;
        }

        CustomSettings read = new CustomSettings(overworld, nether, end);
        return isWrapped(read) ? read : null;
    }

    private static @Nullable DimensionShape readShape(WorldDimensions dimensions, ResourceKey<LevelStem> key) {
        LoopSpans spans = ShapeDimensions.spansOf(dimensions, key);
        if (spans == null) {
            return ShapedDimensions.carriedShapeOf(dimensions, key) == null
                    ? new DimensionShape(null, CustomSettings.DEFAULT_OPTIONS)
                    : null;
        }

        return StemKind.of(spans) == null ? null : new DimensionShape(spans, ShapeDimensions.optionsOf(dimensions, key));
    }

    private static boolean isWrapped(CustomSettings settings) {
        for (VanillaDimension dimension : VanillaDimension.values()) {
            if (settings.get(dimension).spans() != null) {
                return true;
            }
        }

        return false;
    }

    private static WorldDimensions withShape(WorldDimensions dimensions, ResourceKey<LevelStem> key,
            DimensionShape shape) {
        LoopSpans spans = shape.spans();
        return spans == null
                ? dimensions
                : ShapedDimensions.withShape(dimensions, key, carried(spans, shape.options()));
    }

    private static CarriedShape carried(LoopSpans spans, GenerationOptions options) {
        return new CarriedShape(FlatShape.of(spans), options);
    }

    private CustomDimensions() {
    }
}
