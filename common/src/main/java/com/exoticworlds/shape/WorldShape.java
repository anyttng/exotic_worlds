package com.exoticworlds.shape;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.WorldDimensions;

public record WorldShape(Identifier id, AtCreation atCreation, Runnable resetSettings,
        @Nullable FromExisting fromExisting) {
    private static final String KEY_PREFIX = "gui.";
    private static final String KEY_INFIX = ".world_shape.";

    @FunctionalInterface
    public interface AtCreation {
        WorldDimensions apply(RegistryAccess.Frozen registries, WorldDimensions dimensions);
    }

    @FunctionalInterface
    public interface FromExisting {
        boolean tryAdopt(RegistryAccess.Frozen registries, WorldDimensions dimensions);
    }

    public static WorldShape of(Identifier id, AtCreation atCreation) {
        return new WorldShape(id, atCreation, () -> {
        }, null);
    }

    public static WorldShape of(Identifier id, AtCreation atCreation, Runnable resetSettings,
            FromExisting fromExisting) {
        return new WorldShape(id, atCreation, resetSettings, fromExisting);
    }

    public static Component label(Identifier id) {
        return Component.translatable(labelKey(id));
    }

    private static String labelKey(Identifier id) {
        return KEY_PREFIX + id.getNamespace() + KEY_INFIX + id.getPath();
    }
}
