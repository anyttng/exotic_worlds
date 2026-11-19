package com.exoticworlds.shape;

import java.util.Comparator;
import java.util.List;

import com.exoticworlds.api.v2.option.DimensionOption;
import com.exoticworlds.core.StartupRegistry;

public final class DeclaredOptions {
    private static final StartupRegistry<String, DimensionOption<?>> OPTIONS =
            new StartupRegistry<>("Dimension options");

    public static void register(DimensionOption<?> declared) {
        OPTIONS.register(declared.option().key(), declared);
    }

    public static List<DimensionOption<?>> all() {
        return OPTIONS.entries().values().stream()
                .sorted(Comparator.comparingInt((DimensionOption<?> declared) -> declared.option().position())
                        .thenComparing(declared -> declared.option().key()))
                .toList();
    }

    private DeclaredOptions() {
    }
}
