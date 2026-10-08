package com.exoticworlds.shape.cylinder;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.api.v1.shape.ShapeDimensions;
import com.exoticworlds.core.WorldLoopSizes;
import com.exoticworlds.shape.ShapeStems;

import net.minecraft.core.Direction;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;

public final class CylinderDimensions {

    public static WorldDimensions apply(WorldDimensions dimensions, CylinderSettings settings) {
        return ShapeDimensions.withSpans(dimensions,
                settings.overworld(),
                ShapeStems.netherSpans(settings.overworld(), settings.netherScale()),
                settings.end(),
                settings.generationOptions());
    }

    public static @Nullable CylinderSettings read(WorldDimensions dimensions) {
        LoopSpans overworld = ShapeStems.spansOf(dimensions, LevelStem.OVERWORLD, CylinderSettings::isCylinder);
        if (overworld == null) {
            return null;
        }

        Direction.Axis axis = CylinderSettings.loopedAxis(overworld);
        return new CylinderSettings(
                overworld,
                ShapeStems.readNetherScale(dimensions, CylinderSettings::isCylinder, overworld, axis),
                readEndSpans(dimensions, axis),
                ShapeDimensions.optionsOf(dimensions, LevelStem.OVERWORLD));
    }

    private static LoopSpans readEndSpans(WorldDimensions dimensions, Direction.Axis axis) {
        LoopSpans end = ShapeStems.spansOf(dimensions, LevelStem.END, CylinderSettings::isCylinder);
        return end != null && end.loops(axis)
                ? end
                : LoopSpans.ofWidth(axis, WorldLoopSizes.END_DEFAULT_CHUNK_WIDTH);
    }

    private CylinderDimensions() {
    }
}
