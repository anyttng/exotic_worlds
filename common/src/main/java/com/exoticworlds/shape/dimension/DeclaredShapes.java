package com.exoticworlds.shape.dimension;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.ExoticWorlds;
import com.exoticworlds.core.feature.ShapeFeatures;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.registry.StartupRegistry;
import com.exoticworlds.core.world.ShapeDerivations;

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
        ShapeDerivations.register(shape.getId(), shape::derive);
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

    public static void warnOfUnknownShapes() {
        unknownShapes(ShapeFeatures.getShapes(), SHAPES.getEntries().keySet()).forEach(
                unknown -> ExoticWorlds.LOGGER.warn("[ew/feature] unknown_shape shape={}", unknown));
    }

    static List<Identifier> unknownShapes(Collection<Identifier> attachedTo, Set<Identifier> known) {
        return attachedTo.stream().filter(shape -> !known.contains(shape)).toList();
    }

    private DeclaredShapes() {
    }
}
