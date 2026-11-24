package com.exoticworlds.bridge.v1.shape;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.bridge.v1.option.OptionBridge;
import com.exoticworlds.core.world.CarriedShape;
import com.exoticworlds.core.world.CoordinateConstants;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.WorldLoopBounds;
import com.exoticworlds.core.world.WorldLoopBounds.AxisBounds;
import com.exoticworlds.engine.gen.ShapedDimensions;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;

public final class ShapeBridge {

    public static WorldDimensions withSpans(WorldDimensions dimensions, LoopSpans overworld, LoopSpans nether,
            LoopSpans end, GenerationOptions options) {
        return ShapedDimensions.withShapes(dimensions,
                carried(overworld, options),
                carried(nether, options),
                carried(end, options));
    }

    public static @Nullable LoopSpans spansOf(WorldDimensions dimensions, ResourceKey<LevelStem> key) {
        FlatShape shape = ShapedDimensions.getShape(dimensions, key);
        return shape == null || shape.mirror() != null ? null : spansOf(shape);
    }

    public static GenerationOptions optionsOf(WorldDimensions dimensions, ResourceKey<LevelStem> key) {
        CarriedShape carried = ShapedDimensions.getCarriedShape(dimensions, key);
        return carried == null ? GenerationOptions.DEFAULT : OptionBridge.toV1(carried.getOptions());
    }

    private static CarriedShape carried(LoopSpans spans, GenerationOptions options) {
        return new CarriedShape(flatShapeOf(spans), OptionBridge.fromV1(options));
    }

    private static LoopSpans spansOf(FlatShape shape) {
        LoopSpans spans = LoopSpans.NONE;
        for (Direction.Axis axis : CoordinateConstants.HORIZONTAL_AXES) {
            if (shape.bounds().getAxis(axis) instanceof AxisBounds.Looped looped) {
                spans = spans.and(axis, looped.minChunk(), looped.maxChunk());
            }
        }

        return spans.withSkew(shape.skewChunks());
    }

    static FlatShape flatShapeOf(LoopSpans spans) {
        WorldLoopBounds bounds = WorldLoopBounds.UNBOUNDED;
        for (Direction.Axis axis : CoordinateConstants.HORIZONTAL_AXES) {
            if (spans.loops(axis)) {
                bounds = bounds.with(axis, new AxisBounds.Looped(spans.minChunk(axis), spans.maxChunk(axis)));
            }
        }

        return FlatShape.latticeTorus(bounds, spans.skewChunks());
    }

    private ShapeBridge() {
    }
}
