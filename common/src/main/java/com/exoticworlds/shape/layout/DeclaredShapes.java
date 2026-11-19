package com.exoticworlds.shape.layout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.ExoticWorlds;
import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.api.v2.option.DimensionOption;
import com.exoticworlds.api.v2.shape.BuiltInShapes;
import com.exoticworlds.api.v2.shape.DimensionContext;
import com.exoticworlds.api.v2.shape.DimensionShape;
import com.exoticworlds.core.StartupRegistry;
import com.exoticworlds.shape.DeclaredOptions;

import net.minecraft.resources.ResourceLocation;

public final class DeclaredShapes {
    private static final StartupRegistry<ResourceLocation, DimensionShape> SHAPES =
            new StartupRegistry<>("Dimension shapes");

    public static void register(DimensionShape shape) {
        SHAPES.register(shape.id(), shape);
    }

    public static List<DimensionShape> all() {
        return List.copyOf(SHAPES.entries().values());
    }

    public static @Nullable DimensionShape of(ResourceLocation id) {
        return SHAPES.entries().get(id);
    }

    public static @Nullable LoopSpans resolve(DimensionShape shape, DimensionContext context) {
        LoopSpans spans = shape.spans(context);
        return ShapeKind.of(spans) == ShapeKind.NORMAL ? null : spans;
    }

    public static void warnOfUnknownOptionShapes() {
        Set<ResourceLocation> known = new HashSet<>(SHAPES.entries().keySet());
        known.add(BuiltInShapes.TORUS);
        known.add(BuiltInShapes.CYLINDER);
        unknownOptionShapes(DeclaredOptions.all(), known).forEach(
                unknown -> ExoticWorlds.LOGGER.warn("[ew/option] unknown_shape {}", unknown));
    }

    static List<String> unknownOptionShapes(Collection<DimensionOption<?>> options, Set<ResourceLocation> known) {
        List<String> unknown = new ArrayList<>();
        for (DimensionOption<?> declared : options) {
            declared.shapes().stream()
                    .filter(shape -> !known.contains(shape))
                    .forEach(shape -> unknown.add("option=" + declared.option().key() + " shape=" + shape));
        }

        return unknown;
    }

    private DeclaredShapes() {
    }
}
