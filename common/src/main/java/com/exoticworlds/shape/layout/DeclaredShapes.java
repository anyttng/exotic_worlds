package com.exoticworlds.shape.layout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.ExoticWorlds;
import com.exoticworlds.api.v2.option.DimensionOption;
import com.exoticworlds.api.v2.option.ShapeOptions;
import com.exoticworlds.api.v2.shape.DimensionContext;
import com.exoticworlds.api.v2.shape.DimensionShape;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.StartupRegistry;
import com.exoticworlds.shape.DeclaredOptions;
import com.exoticworlds.shape.ShapeSpansAdapter;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

public final class DeclaredShapes {
    private static final String TORUS_PATH = "torus";
    private static final String CYLINDER_PATH = "cylinder";

    public static final Identifier TORUS = Identifier.fromNamespaceAndPath(ExoticWorlds.MODID, TORUS_PATH);
    public static final Identifier CYLINDER = Identifier.fromNamespaceAndPath(ExoticWorlds.MODID, CYLINDER_PATH);

    private static final StartupRegistry<Identifier, DimensionShape> SHAPES =
            new StartupRegistry<>("Dimension shapes");

    public static void register(DimensionShape shape) {
        SHAPES.register(shape.id(), shape);
    }

    public static List<DimensionShape> all() {
        return List.copyOf(SHAPES.entries().values());
    }

    public static @Nullable DimensionShape of(Identifier id) {
        return SHAPES.entries().get(id);
    }

    public static DimensionContext context(ResourceKey<LevelStem> dimension, ShapeOptions options,
            @Nullable FlatShape overworld, double overworldScale, double coordinateScale) {
        return new DimensionContext(dimension, options,
                overworld == null ? null : ShapeSpansAdapter.loopSpans(overworld), overworldScale, coordinateScale);
    }

    public static @Nullable FlatShape resolve(DimensionShape shape, DimensionContext context) {
        FlatShape resolved = ShapeSpansAdapter.flatShape(shape.spans(context));
        return ShapeKind.of(resolved) == ShapeKind.NORMAL ? null : resolved;
    }

    public static void warnOfUnknownOptionShapes() {
        Set<Identifier> known = new HashSet<>(SHAPES.entries().keySet());
        known.add(TORUS);
        known.add(CYLINDER);
        unknownOptionShapes(DeclaredOptions.all(), known).forEach(
                unknown -> ExoticWorlds.LOGGER.warn("[ew/option] unknown_shape {}", unknown));
    }

    static List<String> unknownOptionShapes(Collection<DimensionOption<?>> options, Set<Identifier> known) {
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
