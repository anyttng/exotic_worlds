package com.exoticworlds.shape.layout;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.api.v1.shape.ShapeDimensions;
import com.exoticworlds.core.CarriedShape;
import com.exoticworlds.core.DimensionTable;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.NetherScales;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;
import com.exoticworlds.engine.gen.ShapedDimensions;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;

public final class LayoutDimensions {

    public static WorldDimensions apply(WorldDimensions dimensions, WorldLayout layout) {
        WorldDimensions shaped = withShape(dimensions, LevelStem.OVERWORLD, layout.overworld().spans(),
                layout.overworld().options());
        shaped = withShape(shaped, LevelStem.NETHER, layout.netherSpans(), layout.nether().options());
        shaped = withShape(shaped, LevelStem.END, layout.endSpans(), layout.end().options());
        return layout.modDimensions().isEmpty()
                ? shaped
                : ShapedDimensions.withDimensionTable(shaped, tableOf(layout.modDimensions()));
    }

    public static @Nullable WorldLayout read(WorldDimensions dimensions) {
        DimensionLayout overworld = readStem(dimensions, LevelStem.OVERWORLD);
        DimensionLayout nether = readStem(dimensions, LevelStem.NETHER);
        DimensionLayout end = readStem(dimensions, LevelStem.END);
        Map<ResourceKey<LevelStem>, DimensionLayout> modDimensions =
                readTable(ShapedDimensions.dimensionTableOf(dimensions));
        if (overworld == null || nether == null || end == null || modDimensions == null) {
            return null;
        }

        boolean wrapped = overworld.spans() != null || nether.spans() != null || end.spans() != null;
        return wrapped || !modDimensions.isEmpty() ? derived(overworld, nether, end, modDimensions) : null;
    }

    public static WorldLayout ofRows(Map<ResourceKey<LevelStem>, DimensionTable.Entry> rows) {
        Map<ResourceKey<LevelStem>, DimensionLayout> modDimensions = new HashMap<>();
        rows.forEach((key, entry) -> {
            DimensionLayout layout = DimensionTable.isVanillaStem(key) || entry.fromOverworld() ? null : layoutOf(entry);
            if (layout != null) {
                modDimensions.put(key, layout);
            }
        });
        return derived(vanillaRow(rows, LevelStem.OVERWORLD), vanillaRow(rows, LevelStem.NETHER),
                vanillaRow(rows, LevelStem.END), modDimensions);
    }

    private static DimensionLayout vanillaRow(Map<ResourceKey<LevelStem>, DimensionTable.Entry> rows,
            ResourceKey<LevelStem> key) {
        DimensionTable.Entry entry = rows.get(key);
        DimensionLayout layout = entry == null ? null : layoutOf(entry);
        return layout != null ? layout : DimensionLayout.normal(DimensionLayout.DEFAULT_OPTIONS);
    }

    private static WorldLayout derived(DimensionLayout overworld, DimensionLayout nether, DimensionLayout end,
            Map<ResourceKey<LevelStem>, DimensionLayout> modDimensions) {
        LoopSpans spans = overworld.spans();
        if (spans == null) {
            return new WorldLayout(overworld, nether, end, modDimensions, NetherScales.DEFAULT);
        }

        Integer netherScale = OverworldDerivation.netherScaleOf(spans, nether.spans());
        LoopSpans derivedEnd = OverworldDerivation.end(spans);
        return new WorldLayout(overworld,
                netherScale == null ? nether : DimensionLayout.fromOverworld(nether.options()),
                derivedEnd != null && Objects.equals(derivedEnd, end.spans()) ?DimensionLayout.fromOverworld(end.options()) : end,
                modDimensions, netherScale == null ? NetherScales.DEFAULT : netherScale);
    }

    private static @Nullable DimensionLayout readStem(WorldDimensions dimensions, ResourceKey<LevelStem> key) {
        LoopSpans spans = ShapeDimensions.spansOf(dimensions, key);
        if (spans == null) {
            return ShapedDimensions.carriedShapeOf(dimensions, key) == null
                    ? DimensionLayout.normal(DimensionLayout.DEFAULT_OPTIONS)
                    : null;
        }

        return DimensionLayout.looped(spans, ShapeDimensions.optionsOf(dimensions, key));
    }

    private static @Nullable Map<ResourceKey<LevelStem>, DimensionLayout> readTable(DimensionTable table) {
        Map<ResourceKey<LevelStem>, DimensionLayout> modDimensions = new HashMap<>();
        for (Map.Entry<ResourceKey<LevelStem>, DimensionTable.Entry> row : table.entries().entrySet()) {
            if (row.getValue().fromOverworld()) {
                continue;
            }

            DimensionLayout layout = layoutOf(row.getValue());
            if (layout == null) {
                return null;
            }

            modDimensions.put(row.getKey(), layout);
        }

        return modDimensions;
    }

    private static @Nullable DimensionLayout layoutOf(DimensionTable.Entry entry) {
        CarriedShape carried = entry.carried();
        if (carried == null) {
            return DimensionLayout.normal(DimensionLayout.DEFAULT_OPTIONS);
        }

        if (!(carried.shape() instanceof FlatShape shape) || shape.mirror() != null) {
            return null;
        }

        return DimensionLayout.looped(spansOf(shape), carried.generationOptions());
    }

    static LoopSpans spansOf(FlatShape shape) {
        return and(and(LoopSpans.NONE, Direction.Axis.X, shape.bounds().x()), Direction.Axis.Z, shape.bounds().z())
                .withSkew(shape.skewChunks());
    }

    private static LoopSpans and(LoopSpans spans, Direction.Axis axis, AxisBounds bounds) {
        return bounds instanceof AxisBounds.Looped looped ? spans.and(axis, looped.minChunk(), looped.maxChunk()) : spans;
    }

    private static DimensionTable tableOf(Map<ResourceKey<LevelStem>, DimensionLayout> modDimensions) {
        Map<ResourceKey<LevelStem>, DimensionTable.Entry> entries = new HashMap<>();
        modDimensions.forEach((key, layout) -> entries.put(key, entryOf(layout)));
        return DimensionTable.of(entries);
    }

    public static DimensionTable.Entry entryOf(DimensionLayout layout) {
        if (layout.kind() == ShapeKind.FROM_OVERWORLD) {
            return DimensionTable.Entry.FROM_OVERWORLD;
        }

        LoopSpans spans = layout.spans();
        return spans == null
                ? DimensionTable.Entry.UNWRAPPED
                : new DimensionTable.Entry(carried(spans, layout.options()));
    }

    private static WorldDimensions withShape(WorldDimensions dimensions, ResourceKey<LevelStem> key,
            @Nullable LoopSpans spans, GenerationOptions options) {
        return spans == null ? dimensions : ShapedDimensions.withShape(dimensions, key, carried(spans, options));
    }

    private static CarriedShape carried(LoopSpans spans, GenerationOptions options) {
        return new CarriedShape(FlatShape.of(spans), options);
    }

    private LayoutDimensions() {
    }
}
