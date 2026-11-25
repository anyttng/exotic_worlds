package com.exoticworlds.shape.option;

import java.util.Comparator;
import java.util.List;

import com.exoticworlds.core.feature.DimensionScope;
import com.exoticworlds.core.registry.StartupRegistry;

import net.minecraft.resources.Identifier;

public final class DeclaredOptions {
    private static final StartupRegistry<String, Entry> OPTIONS = new StartupRegistry<>("Dimension options");

    public record Entry(DimensionOption<?> option, List<Identifier> shapes, DimensionScope scope) {
        public Entry {
            shapes = List.copyOf(shapes);
        }

        public boolean isForShape(Identifier shape) {
            return this.shapes.isEmpty() || this.shapes.contains(shape);
        }
    }

    public static void register(Entry declared) {
        OPTIONS.register(declared.option().getOption().key(), declared);
    }

    public static List<Entry> getAll() {
        return OPTIONS.getEntries().values().stream()
                .sorted(Comparator.comparingInt((Entry declared) -> declared.option().getOption().position())
                        .thenComparing(declared -> declared.option().getOption().key()))
                .toList();
    }

    private DeclaredOptions() {
    }
}
