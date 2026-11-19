package com.exoticworlds.api.v2.client;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.api.v2.option.DimensionOption;
import com.exoticworlds.core.StartupRegistry;

import net.minecraft.network.chat.Component;

/**
 * The line of text the World Shape Settings screen shows under a {@link DimensionOption}'s controls: what the chosen
 * value comes to on the dimension being edited. An option needs none; one that has it registers it from the client
 * side alone, before the registration boundary closes.
 */
public final class DimensionOptionLines {

    /**
     * States what {@code value} comes to on a dimension of these {@code spans}. An empty component shows no line.
     *
     * @param <T> the option's value type
     */
    @FunctionalInterface
    public interface Line<T> {
        Component line(T value, LoopSpans spans);
    }

    private static final StartupRegistry<String, Line<?>> LINES = new StartupRegistry<>("Dimension option lines");

    /**
     * Registers the line shown under {@code option}.
     *
     * @throws IllegalStateException if the registration boundary has already closed, or a line is already registered
     *         for this option
     */
    public static <T> void register(DimensionOption<T> option, Line<T> line) {
        LINES.register(option.option().key(), line);
    }

    /** The line registered for {@code option}, or {@code null} where it has none. */
    @SuppressWarnings("unchecked")
    public static <T> @Nullable Line<T> of(DimensionOption<T> option) {
        return (Line<T>) LINES.entries().get(option.option().key());
    }

    private DimensionOptionLines() {
    }
}
