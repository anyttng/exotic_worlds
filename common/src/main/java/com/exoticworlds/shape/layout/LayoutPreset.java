package com.exoticworlds.shape.layout;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.option.ShapeOption;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.shape.WorldLoopPresets;
import com.exoticworlds.shape.dimension.DeclaredShapes;
import com.exoticworlds.shape.dimension.DimensionContext;
import com.exoticworlds.shape.dimension.DimensionShape;
import com.exoticworlds.shape.option.DimensionOption;
import com.exoticworlds.shape.torus.Torus;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.dimension.LevelStem;

public final class LayoutPreset {

    public static boolean isOffered(WorldLayout layout) {
        return getPresetShape(layout.overworld()) != null;
    }

    public static WorldLayout apply(WorldLayout layout, WorldLoopPresets preset, double overworldScale) {
        DimensionLayout overworld = sized(layout.overworld(), preset, overworldScale);
        return overworld == null
                ? layout
                : new WorldLayout(overworld,
                        DimensionLayout.fromOverworld(layout.nether().options()),
                        DimensionLayout.fromOverworld(layout.end().options()),
                        layout.modDimensions(), preset.getNetherScale());
    }

    public static boolean isMatching(WorldLayout layout, WorldLoopPresets preset) {
        DimensionLayout overworld = layout.overworld();
        if (layout.nether().kind() != ShapeKind.FROM_OVERWORLD || layout.end().kind() != ShapeKind.FROM_OVERWORLD
                || layout.netherScale() != preset.getNetherScale() || overworld.shape() == null) {
            return false;
        }

        DimensionShape shape = getPresetShape(overworld);
        if (shape == null) {
            return false;
        }

        ShapeOptions current = getCurrentOptions(shape, overworld);
        return shape.withPreset(preset, current).equals(current);
    }

    private static @Nullable DimensionLayout sized(DimensionLayout overworld, WorldLoopPresets preset,
            double overworldScale) {
        DimensionShape shape = getPresetShape(overworld);
        if (shape == null) {
            return null;
        }

        ShapeOptions options = shape.withPreset(preset, getCurrentOptions(shape, overworld));
        FlatShape spans = DeclaredShapes.resolve(shape,
                new DimensionContext(LevelStem.OVERWORLD, options, null, overworldScale, overworldScale));
        ShapeOptions stored = options;
        for (DimensionOption<?> parameter : shape.getParameters()) {
            stored = reset(parameter.getOption(), stored);
        }

        return DimensionLayout.shaped(shape.getId(), spans, stored);
    }

    private static ShapeOptions getCurrentOptions(DimensionShape shape, DimensionLayout overworld) {
        FlatShape spans = overworld.spans();
        ShapeOptions read = spans == null || overworld.shape() == null
                ? ShapeOptions.DEFAULT
                : shape.optionsFor(spans).orElse(ShapeOptions.DEFAULT);
        ShapeOptions current = overworld.options();
        for (DimensionOption<?> parameter : shape.getParameters()) {
            current = copied(parameter.getOption(), read, current);
        }

        return current;
    }

    private static @Nullable DimensionShape getPresetShape(DimensionLayout overworld) {
        Identifier chosen = overworld.shape();
        DimensionShape shape = chosen == null ? Torus.SHAPE : DeclaredShapes.of(chosen);
        return shape != null && shape.canTakePresets() ? shape : null;
    }

    private static <T> ShapeOptions copied(ShapeOption<T> option, ShapeOptions from, ShapeOptions into) {
        return into.with(option, from.get(option));
    }

    private static <T> ShapeOptions reset(ShapeOption<T> option, ShapeOptions options) {
        return options.with(option, option.defaultValue());
    }

    private LayoutPreset() {
    }
}
