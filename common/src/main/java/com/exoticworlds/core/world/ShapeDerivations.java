package com.exoticworlds.core.world;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.registry.StartupRegistry;

import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

public final class ShapeDerivations {
    private static final StartupRegistry<Identifier, Rule> RULES = new StartupRegistry<>("Shape derivations");

    @FunctionalInterface
    public interface Rule {
        @Nullable FlatShape derive(FlatShape overworld, ResourceKey<LevelStem> dimension, double overworldScale,
                double coordinateScale);
    }

    public static void register(Identifier shape, Rule rule) {
        RULES.register(shape, rule);
    }

    public static @Nullable FlatShape derive(@Nullable Identifier overworldShape, @Nullable FlatShape overworld,
            ResourceKey<LevelStem> dimension, double overworldScale, double coordinateScale) {
        if (overworld == null || overworld.mirror() != null || !isLooping(overworld)) {
            return null;
        }

        Rule rule = overworldShape == null ? null : RULES.getEntries().get(overworldShape);
        return rule != null
                ? rule.derive(overworld, dimension, overworldScale, coordinateScale)
                : LegacyDerivation.derive(overworld, overworldScale, coordinateScale);
    }

    private static boolean isLooping(FlatShape spans) {
        return spans.bounds().isLooped(Direction.Axis.X) || spans.bounds().isLooped(Direction.Axis.Z);
    }

    private ShapeDerivations() {
    }
}
