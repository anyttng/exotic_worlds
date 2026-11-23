package com.exoticworlds.core;

import java.util.Comparator;
import java.util.List;

public final class ShapeOptionRegistry {
    private static final StartupRegistry<String, ShapeOption<?>> KEYS = new StartupRegistry<>("Shape options");

    public static void register(ShapeOption<?> key) {
        KEYS.register(key.key(), key);
    }

    public static List<ShapeOption<?>> all() {
        return KEYS.entries().values().stream()
                .sorted(Comparator.comparingInt((ShapeOption<?> key) -> key.position()).thenComparing(ShapeOption::key))
                .toList();
    }

    private ShapeOptionRegistry() {
    }
}
