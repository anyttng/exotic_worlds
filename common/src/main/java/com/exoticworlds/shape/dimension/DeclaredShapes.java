package com.exoticworlds.shape.dimension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.ExoticWorlds;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.registry.StartupRegistry;
import com.exoticworlds.shape.option.DeclaredOptions;
import com.exoticworlds.shape.option.DimensionOption;

import net.minecraft.resources.Identifier;

public final class DeclaredShapes {
    private static final String TORUS_PATH = "torus";
    private static final String CYLINDER_PATH = "cylinder";

    public static final Identifier TORUS = Identifier.fromNamespaceAndPath(ExoticWorlds.MODID, TORUS_PATH);
    public static final Identifier CYLINDER = Identifier.fromNamespaceAndPath(ExoticWorlds.MODID, CYLINDER_PATH);

    private static final StartupRegistry<Identifier, DimensionShape> SHAPES =
            new StartupRegistry<>("Dimension shapes");

    public static void register(DimensionShape shape) {
        SHAPES.register(shape.getId(), shape);
    }

    public static List<DimensionShape> getAll() {
        return List.copyOf(SHAPES.getEntries().values());
    }

    public static @Nullable DimensionShape of(Identifier id) {
        return SHAPES.getEntries().get(id);
    }

    public static @Nullable FlatShape resolve(DimensionShape shape, DimensionContext context) {
        FlatShape resolved = shape.spans(context);
        return resolved.getIdentification() == FlatShape.Identification.RECTANGLE ? null : resolved;
    }

    public static void warnOfUnknownOptionShapes() {
        Set<Identifier> known = new HashSet<>(SHAPES.getEntries().keySet());
        known.add(TORUS);
        known.add(CYLINDER);
        unknownOptionShapes(DeclaredOptions.getAll(), known).forEach(
                unknown -> ExoticWorlds.LOGGER.warn("[ew/option] unknown_shape {}", unknown));
    }

    static List<String> unknownOptionShapes(Collection<DimensionOption<?>> options, Set<Identifier> known) {
        List<String> unknown = new ArrayList<>();
        for (DimensionOption<?> declared : options) {
            declared.getShapes().stream()
                    .filter(shape -> !known.contains(shape))
                    .forEach(shape -> unknown.add("option=" + declared.getOption().key() + " shape=" + shape));
        }

        return unknown;
    }

    private DeclaredShapes() {
    }
}
