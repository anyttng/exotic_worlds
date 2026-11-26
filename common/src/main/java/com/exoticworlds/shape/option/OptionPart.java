package com.exoticworlds.shape.option;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.world.StemTerrain;

import net.minecraft.network.chat.Component;

public sealed interface OptionPart<T>
        permits OptionPart.Toggle, OptionPart.Choice, OptionPart.WholeNumber, OptionPart.Row {
    String OWN_CAPTION = "";

    String key();

    Predicate<T> shownWhen();

    record Toggle<T>(String key, Function<T, Boolean> read, BiFunction<T, Boolean, T> write, Predicate<T> shownWhen)
            implements OptionPart<T> {
    }

    record Choice<T, V>(String key, List<V> values, Function<V, String> valueKey,
            Function<V, @Nullable Component> ownName, Function<T, V> read, BiFunction<T, V, T> write,
            Predicate<T> shownWhen) implements OptionPart<T> {
        public Choice {
            values = List.copyOf(values);
        }
    }

    record WholeNumber<T>(String key, BiFunction<StemTerrain, T, NumberRange> range,
            Function<T, Integer> read, BiFunction<T, Integer, T> write, BiFunction<T, Integer, Component> valueLine,
            Predicate<T> shownWhen) implements OptionPart<T> {
        public WholeNumber(String key, int min, int max, Function<T, Integer> read, BiFunction<T, Integer, T> write,
                Predicate<T> shownWhen) {
            this(key, fixed(new NumberRange(min, max)), read, write, (value, number) -> Component.empty(), shownWhen);
        }

        private static <T> BiFunction<StemTerrain, T, NumberRange> fixed(NumberRange range) {
            return (stem, value) -> range;
        }
    }

    record Row<T>(List<OptionPart<T>> parts) implements OptionPart<T> {
        public Row {
            parts = List.copyOf(parts);
        }

        @Override
        public String key() {
            return OWN_CAPTION;
        }

        @Override
        public Predicate<T> shownWhen() {
            return value -> true;
        }
    }

    static OptionPart<Boolean> toggle() {
        return new Toggle<>(OWN_CAPTION, value -> value, (value, picked) -> picked, value -> true);
    }
}
