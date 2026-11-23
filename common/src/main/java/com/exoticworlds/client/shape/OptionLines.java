package com.exoticworlds.client.shape;

import java.util.function.BiFunction;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.ShapeOption;
import com.exoticworlds.core.StartupRegistry;

import net.minecraft.network.chat.Component;

public final class OptionLines {
    private static final StartupRegistry<String, BiFunction<?, FlatShape, Component>> LINES =
            new StartupRegistry<>("Dimension option lines");

    public static <T> void register(ShapeOption<T> option, BiFunction<T, FlatShape, Component> line) {
        LINES.register(option.key(), line);
    }

    @SuppressWarnings("unchecked")
    public static <T> @Nullable BiFunction<T, FlatShape, Component> of(ShapeOption<T> option) {
        return (BiFunction<T, FlatShape, Component>) LINES.entries().get(option.key());
    }

    private OptionLines() {
    }
}
