package com.exoticworlds.shape.torus;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.api.v1.shape.ShapeDimensions;
import com.exoticworlds.shape.ShapeStems;

import net.minecraft.core.Direction;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;

public final class TorusDimensions {

    public static WorldDimensions apply(WorldDimensions dimensions, TorusSettings settings) {
        return ShapeStems.withSpans(dimensions, settings);
    }

    public static @Nullable TorusSettings read(WorldDimensions dimensions) {
        LoopSpans overworld = ShapeStems.spansOf(dimensions, LevelStem.OVERWORLD, TorusSettings::isTorus);
        if (overworld == null) {
            return null;
        }

        return new TorusSettings(
                overworld,
                ShapeStems.readNetherScale(dimensions, TorusSettings::isTorus, overworld, Direction.Axis.X),
                readEndSpans(dimensions),
                ShapeDimensions.optionsOf(dimensions, LevelStem.OVERWORLD));
    }

    private static LoopSpans readEndSpans(WorldDimensions dimensions) {
        LoopSpans end = ShapeStems.spansOf(dimensions, LevelStem.END, TorusSettings::isPlainSquare);
        return end != null ? end : TorusSettings.DEFAULT.end();
    }

    private TorusDimensions() {
    }
}
