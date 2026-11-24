package com.exoticworlds.shape.layout;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.world.CarriedShape;
import com.exoticworlds.core.world.DimensionTable;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.NetherScales;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.engine.gen.ShapedDimensions;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;

public final class LayoutDimensions {

    public static WorldDimensions apply(WorldDimensions dimensions, WorldLayout layout) {
        WorldDimensions shaped = withShape(dimensions, LevelStem.OVERWORLD, layout.overworld().spans(),
                layout.overworld().options());
        shaped = withShape(shaped, LevelStem.NETHER, layout.getNetherSpans(), layout.nether().options());
        shaped = withShape(shaped, LevelStem.END, layout.getEndSpans(), layout.end().options());
        return layout.modDimensions().isEmpty()
                ? shaped
                : ShapedDimensions.withDimensionTable(shaped, tableOf(layout.modDimensions()));
    }

    public static @Nullable WorldLayout read(WorldDimensions dimensions) {
        DimensionLayout overworld = readStem(dimensions, LevelStem.OVERWORLD);
        DimensionLayout nether = readStem(dimensions, LevelStem.NETHER);
        DimensionLayout end = readStem(dimensions, LevelStem.END);
        Map<ResourceKey<LevelStem>, DimensionLayout> modDimensions =
                readTable(ShapedDimensions.getDimensionTable(dimensions));
        if (overworld == null || nether == null || end == null || modDimensions == null) {
            return null;
        }

        boolean isWrapped = overworld.spans() != null || nether.spans() != null || end.spans() != null;
        return isWrapped || !modDimensions.isEmpty() ? derived(overworld, nether, end, modDimensions) : null;
    }

    public static WorldLayout ofRows(Map<ResourceKey<LevelStem>, DimensionTable.Entry> rows) {
        Map<ResourceKey<LevelStem>, DimensionLayout> modDimensions = new HashMap<>();
        rows.forEach((key, entry) -> {
            DimensionLayout layout = DimensionTable.isVanillaStem(key) || entry.isFromOverworld() ? null : layoutOf(entry);
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
        FlatShape spans = overworld.spans();
        if (spans == null) {
            return new WorldLayout(overworld, nether, end, modDimensions, NetherScales.DEFAULT);
        }

        Integer netherScale = OverworldDerivation.netherScaleOf(spans, nether.spans());
        FlatShape derivedEnd = OverworldDerivation.end(spans);
        return new WorldLayout(overworld,
                netherScale == null ? nether : DimensionLayout.fromOverworld(nether.options()),
                derivedEnd != null && Objects.equals(derivedEnd, end.spans()) ?DimensionLayout.fromOverworld(end.options()) : end,
                modDimensions, netherScale == null ? NetherScales.DEFAULT : netherScale);
    }

    private static @Nullable DimensionLayout readStem(WorldDimensions dimensions, ResourceKey<LevelStem> key) {
        FlatShape stored = ShapedDimensions.getShape(dimensions, key);
        FlatShape spans = stored == null || stored.mirror() != null ? null : stored;
        if (spans == null) {
            return ShapedDimensions.getCarriedShape(dimensions, key) == null
                    ? DimensionLayout.normal(DimensionLayout.DEFAULT_OPTIONS)
                    : null;
        }

        CarriedShape carried = ShapedDimensions.getCarriedShape(dimensions, key);
        return DimensionLayout.looped(spans, carried == null ? ShapeOptions.DEFAULT : carried.getOptions());
    }

    private static @Nullable Map<ResourceKey<LevelStem>, DimensionLayout> readTable(DimensionTable table) {
        Map<ResourceKey<LevelStem>, DimensionLayout> modDimensions = new HashMap<>();
        for (Map.Entry<ResourceKey<LevelStem>, DimensionTable.Entry> row : table.getEntries().entrySet()) {
            if (row.getValue().isFromOverworld()) {
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

        FlatShape shape = carried.getShape();
        if (shape.mirror() != null) {
            return null;
        }

        return DimensionLayout.looped(shape, carried.getOptions());
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

        FlatShape spans = layout.spans();
        return spans == null
                ? DimensionTable.Entry.UNWRAPPED
                : new DimensionTable.Entry(carried(spans, layout.options()));
    }

    private static WorldDimensions withShape(WorldDimensions dimensions, ResourceKey<LevelStem> key,
            @Nullable FlatShape spans, ShapeOptions options) {
        return spans == null ? dimensions : ShapedDimensions.withShape(dimensions, key, carried(spans, options));
    }

    private static CarriedShape carried(FlatShape spans, ShapeOptions options) {
        return new CarriedShape(spans, options);
    }

    private LayoutDimensions() {
    }
}
