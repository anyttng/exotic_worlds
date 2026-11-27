package com.exoticworlds.core.option;

import java.util.Comparator;
import java.util.List;

import com.exoticworlds.core.registry.StartupRegistry;

public final class ShapeOptionRegistry {
    private static final StartupRegistry<String, ShapeOption<?>> KEYS = new StartupRegistry<>("Shape options");

    public static void register(ShapeOption<?> key) {
        KEYS.register(key.key(), key);
    }

    public static List<ShapeOption<?>> getAll() {
        return KEYS.getEntries().values().stream()
                .sorted(Comparator.comparingInt((ShapeOption<?> key) -> key.position()).thenComparing(ShapeOption::key))
                .toList();
    }

    private ShapeOptionRegistry() {
    }
}
