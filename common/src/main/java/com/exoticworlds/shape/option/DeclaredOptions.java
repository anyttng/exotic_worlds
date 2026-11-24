package com.exoticworlds.shape.option;

import java.util.Comparator;
import java.util.List;

import com.exoticworlds.core.registry.StartupRegistry;

public final class DeclaredOptions {
    private static final StartupRegistry<String, DimensionOption<?>> OPTIONS =
            new StartupRegistry<>("Dimension options");

    public static void register(DimensionOption<?> declared) {
        OPTIONS.register(declared.getOption().key(), declared);
    }

    public static List<DimensionOption<?>> getAll() {
        return OPTIONS.getEntries().values().stream()
                .sorted(Comparator.comparingInt((DimensionOption<?> declared) -> declared.getOption().position())
                        .thenComparing(declared -> declared.getOption().key()))
                .toList();
    }

    private DeclaredOptions() {
    }
}
