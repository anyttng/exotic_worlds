package com.exoticworlds.api.v2.option;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import net.minecraft.network.chat.Component;

/**
 * One control of a {@link DimensionOptionV2}, drawn by the World Shape Settings screen: an on/off toggle, a choice among
 * named values, or a bounded whole number. A part edits one piece of the option's value through {@code read} and
 * {@code write}; an option of one simple value has one part that reads and writes the value itself.
 *
 * <p>{@code key} is appended to the option's caption key to name the part ({@code <caption>.<key>}); the empty key
 * means the part carries the option's own caption. {@code shownWhen} hides the part while the option's value does not
 * call for it.</p>
 *
 * @param <T> the option's value type
 */
public sealed interface OptionPartV2<T> permits OptionPartV2.Toggle, OptionPartV2.Choice, OptionPartV2.WholeNumber {
    String OWN_CAPTION = "";

    String key();

    Predicate<T> shownWhen();

    /**
     * An on/off toggle, labelled with the game's own ON and OFF. Its one-line explanations are the translation keys
     * {@code <part caption>.hint.on} and {@code <part caption>.hint.off}.
     */
    record Toggle<T>(String key, Function<T, Boolean> read, BiFunction<T, Boolean, T> write, Predicate<T> shownWhen)
            implements OptionPartV2<T> {
    }

    /**
     * A choice among {@code values}. A value's name is the translation key {@code <part caption>.<valueKey>} and its
     * one-line explanation {@code <part caption>.hint.<valueKey>}. {@code ownName} replaces the name key for a value
     * it answers non-null for, which is how a value takes a word the game already translates.
     */
    record Choice<T, V>(String key, List<V> values, Function<V, String> valueKey,
            Function<V, @Nullable Component> ownName, Function<T, V> read, BiFunction<T, V, T> write,
            Predicate<T> shownWhen) implements OptionPartV2<T> {
        public Choice {
            values = List.copyOf(values);
        }
    }

    /** A whole number from {@code min} to {@code max}, both included. */
    record WholeNumber<T>(String key, int min, int max, Function<T, Integer> read, BiFunction<T, Integer, T> write,
            Predicate<T> shownWhen) implements OptionPartV2<T> {
        public WholeNumber {
            if (min > max) {
                throw new IllegalArgumentException("A number part needs min <= max, got " + min + " and " + max);
            }
        }
    }

    /** The one part of an on/off option. */
    static OptionPartV2<Boolean> toggle() {
        return new Toggle<>(OWN_CAPTION, value -> value, (value, picked) -> picked, value -> true);
    }

    /** The one part of an option whose value is one of {@code values}. */
    static <V> OptionPartV2<V> choice(List<V> values, Function<V, String> valueKey) {
        return new Choice<>(OWN_CAPTION, values, valueKey, value -> null, value -> value, (value, picked) -> picked,
                value -> true);
    }

    /** The one part of an option whose value is a whole number from {@code min} to {@code max}. */
    static OptionPartV2<Integer> number(int min, int max) {
        return new WholeNumber<>(OWN_CAPTION, min, max, value -> value, (value, picked) -> picked, value -> true);
    }
}
