package com.exoticworlds.shape.torus;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.api.v1.shape.ShapeDimensions;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.shape.ShapeStems;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;

public final class TorusDimensions {

    public static WorldDimensions apply(WorldDimensions dimensions, TorusSettings settings) {
        return ShapeStems.withSpans(dimensions, settings);
    }

    public static @Nullable TorusSettings read(WorldDimensions dimensions) {
        LoopSpans overworld = ShapeStems.spansOf(dimensions, LevelStem.OVERWORLD, TorusSettings::isTorus);
        if (overworld == null
                || ShapeStems.carriesDimensionTable(dimensions)
                || !ShapeStems.isOwnOrAbsent(dimensions, LevelStem.NETHER, TorusSettings::isTorus)
                || !ShapeStems.isOwnOrAbsent(dimensions, LevelStem.END, TorusSettings::isTorus)) {
            return null;
        }

        TorusSettings read = new TorusSettings(
                overworld,
                ShapeStems.readNetherScale(dimensions, TorusSettings::isTorus, overworld, Direction.Axis.X),
                readEndSpans(dimensions),
                ShapeDimensions.optionsOf(dimensions, LevelStem.OVERWORLD));
        return carriesSkew(dimensions) || ShapeStems.writesBack(dimensions, read) ? read : null;
    }

    private static boolean carriesSkew(WorldDimensions dimensions) {
        for (ResourceKey<LevelStem> key : List.of(LevelStem.OVERWORLD, LevelStem.NETHER, LevelStem.END)) {
            LoopSpans spans = ShapeDimensions.spansOf(dimensions, key);
            if (spans != null && spans.skewChunks() != FlatShape.NO_SKEW) {
                return true;
            }
        }

        return false;
    }

    private static LoopSpans readEndSpans(WorldDimensions dimensions) {
        LoopSpans end = ShapeStems.spansOf(dimensions, LevelStem.END, TorusSettings::isPlainSquare);
        return end != null ? end : TorusSettings.DEFAULT.end();
    }

    private TorusDimensions() {
    }
}
