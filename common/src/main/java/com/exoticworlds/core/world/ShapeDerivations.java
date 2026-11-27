package com.exoticworlds.core.world;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.registry.StartupRegistry;

import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

public final class ShapeDerivations {
    private static final StartupRegistry<Identifier, Rule> RULES = new StartupRegistry<>("Shape derivations");

    @FunctionalInterface
    public interface Rule {
        @Nullable FlatShape derive(FlatShape overworld, DerivedDimension dimension);
    }

    public static void register(Identifier shape, Rule rule) {
        RULES.register(shape, rule);
    }

    public static @Nullable FlatShape derive(@Nullable Identifier overworldShape, @Nullable ShapeModel overworld,
            DerivedDimension dimension) {
        if (!(overworld instanceof FlatShape flat) || flat.mirror() != null || !isLooping(flat)) {
            return null;
        }

        Rule rule = overworldShape == null ? null : RULES.getEntries().get(overworldShape);
        return rule != null
                ? rule.derive(flat, dimension)
                : LegacyDerivation.derive(flat, dimension.overworldScale(), dimension.coordinateScale());
    }

    private static boolean isLooping(FlatShape spans) {
        return spans.bounds().isLooped(Direction.Axis.X) || spans.bounds().isLooped(Direction.Axis.Z);
    }

    private ShapeDerivations() {
    }
}
