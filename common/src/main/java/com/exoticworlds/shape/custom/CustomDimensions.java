package com.exoticworlds.shape.custom;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.api.v1.shape.ShapeDimensions;
import com.exoticworlds.core.CarriedShape;
import com.exoticworlds.core.DimensionTable;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;
import com.exoticworlds.engine.gen.ShapedDimensions;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;

public final class CustomDimensions {

    public static WorldDimensions apply(WorldDimensions dimensions, CustomSettings settings) {
        WorldDimensions shaped = dimensions;
        for (VanillaDimension dimension : VanillaDimension.values()) {
            shaped = withShape(shaped, dimension.stem(), settings.get(dimension));
        }

        return settings.modded().isEmpty()
                ? shaped
                : ShapedDimensions.withDimensionTable(shaped, tableOf(settings.modded()));
    }

    public static @Nullable CustomSettings read(WorldDimensions dimensions) {
        DimensionShape overworld = readShape(dimensions, LevelStem.OVERWORLD);
        DimensionShape nether = readShape(dimensions, LevelStem.NETHER);
        DimensionShape end = readShape(dimensions, LevelStem.END);
        Map<ResourceKey<LevelStem>, DimensionShape> modded = readModded(ShapedDimensions.dimensionTableOf(dimensions));
        if (overworld == null || nether == null || end == null || modded == null) {
            return null;
        }

        CustomSettings read = new CustomSettings(overworld, nether, end, modded);
        return isWrapped(read) || !modded.isEmpty() ? read : null;
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

    private static @Nullable Map<ResourceKey<LevelStem>, DimensionShape> readModded(DimensionTable table) {
        Map<ResourceKey<LevelStem>, DimensionShape> modded = new HashMap<>();
        for (Map.Entry<ResourceKey<LevelStem>, DimensionTable.Entry> entry : table.entries().entrySet()) {
            DimensionShape shape = shapeOf(entry.getValue());
            if (shape == null) {
                return null;
            }

            modded.put(entry.getKey(), shape);
        }

        return modded;
    }

    private static @Nullable DimensionShape shapeOf(DimensionTable.Entry entry) {
        CarriedShape carried = entry.carried();
        if (carried == null) {
            return new DimensionShape(null, CustomSettings.DEFAULT_OPTIONS);
        }

        FlatShape shape = carried.shape();
        if (shape.mirror() != null || shape.skewChunks() != FlatShape.NO_SKEW) {
            return null;
        }

        LoopSpans spans = and(and(LoopSpans.NONE, Direction.Axis.X, shape.bounds().x()),
                Direction.Axis.Z, shape.bounds().z());
        return new DimensionShape(spans, carried.generationOptions());
    }

    private static LoopSpans and(LoopSpans spans, Direction.Axis axis, AxisBounds bounds) {
        return bounds instanceof AxisBounds.Looped looped ? spans.and(axis, looped.minChunk(), looped.maxChunk()) : spans;
    }

    private static boolean isWrapped(CustomSettings settings) {
        for (VanillaDimension dimension : VanillaDimension.values()) {
            if (settings.get(dimension).spans() != null) {
                return true;
            }
        }

        return false;
    }

    private static DimensionTable tableOf(Map<ResourceKey<LevelStem>, DimensionShape> modded) {
        Map<ResourceKey<LevelStem>, DimensionTable.Entry> entries = new HashMap<>();
        modded.forEach((key, shape) -> {
            LoopSpans spans = shape.spans();
            entries.put(key, spans == null
                    ? DimensionTable.Entry.UNWRAPPED
                    : new DimensionTable.Entry(carried(spans, shape.options())));
        });
        return DimensionTable.of(entries);
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
