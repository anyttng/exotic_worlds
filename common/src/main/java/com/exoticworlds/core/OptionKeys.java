package com.exoticworlds.core;

import java.util.Comparator;
import java.util.List;

public final class OptionKeys {
    private static final StartupRegistry<String, OptionKey<?>> KEYS = new StartupRegistry<>("World option keys");

    public static void register(OptionKey<?> key) {
        KEYS.register(key.key(), key);
    }

    public static List<OptionKey<?>> all() {
        return KEYS.entries().values().stream()
                .sorted(Comparator.comparingInt((OptionKey<?> key) -> key.position()).thenComparing(OptionKey::key))
                .toList();
    }

    private OptionKeys() {
    }
}
