package com.exoticworlds.shape.layout;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.ToDoubleFunction;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v2.shape.DimensionContext;
import com.exoticworlds.api.v2.shape.DimensionShape;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.shape.ShapeOptionsAdapter;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

public final class ShapeRecognition {

    public static WorldLayout recognize(WorldLayout layout, ToDoubleFunction<ResourceKey<LevelStem>> scales) {
        DimensionLayout overworld = recognized(LevelStem.OVERWORLD, layout.overworld(), null, scales);
        FlatShape overworldSpans = overworld.spans();
        Map<ResourceKey<LevelStem>, DimensionLayout> modDimensions = new HashMap<>();
        layout.modDimensions().forEach(
                (key, row) -> modDimensions.put(key, recognized(key, row, overworldSpans, scales)));
        return new WorldLayout(overworld,
                recognized(LevelStem.NETHER, layout.nether(), overworldSpans, scales),
                recognized(LevelStem.END, layout.end(), overworldSpans, scales),
                modDimensions, layout.netherScale());
    }

    private static DimensionLayout recognized(ResourceKey<LevelStem> dimension, DimensionLayout stored,
            @Nullable FlatShape overworld, ToDoubleFunction<ResourceKey<LevelStem>> scales) {
        FlatShape spans = stored.spans();
        if (spans == null || stored.shape() != null) {
            return stored;
        }

        DimensionContext context = DeclaredShapes.context(dimension, ShapeOptionsAdapter.shapeOptions(stored.options()),
                overworld, scales.applyAsDouble(LevelStem.OVERWORLD), scales.applyAsDouble(dimension));
        for (DimensionShape shape : DeclaredShapes.all()) {
            if (Objects.equals(DeclaredShapes.resolve(shape, context), spans)) {
                return DimensionLayout.declared(shape.id(), spans, stored.options());
            }
        }

        return stored;
    }

    private ShapeRecognition() {
    }
}
