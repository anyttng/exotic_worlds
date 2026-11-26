package com.exoticworlds.api.v2.feature;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * One control of an option on the World Shape Settings screen. An option of a plain value has one part made by {@link
 * #toggle()}, {@link #choice} or {@link #number}. An option of a compound value lists several, each reading its piece
 * of the value and writing it back.
 *
 * @param <T> the option's value type
 */
public sealed interface OptionPartV2<T>
        permits OptionPartV2.Toggle, OptionPartV2.Choice, OptionPartV2.WholeNumber, OptionPartV2.Row {
    /** The key of a part that takes the option's own caption. */
    String OWN_CAPTION = "";

    /**
     * The part's key, appended to the option's caption key with a dot, or {@link #OWN_CAPTION} for the caption itself.
     */
    String key();

    /** Whether the part takes input while the option holds the value. A part that does not is drawn disabled. */
    Predicate<T> enabledWhen();

    /**
     * An on/off switch.
     *
     * @param read the switch's state in the value
     * @param write the value with the switch set
     */
    record Toggle<T>(String key, Function<T, Boolean> read, BiFunction<T, Boolean, T> write, Predicate<T> enabledWhen)
            implements OptionPartV2<T> {
    }

    /**
     * A pick of one value from a list, drawn as segments side by side where they fit the row and as a button cycling
     * through them where they do not.
     *
     * @param values the values, in the order they are drawn
     * @param valueKey the key of one value, appended to the part's caption key with a dot for its name and its hint
     * @param read the picked value in the option's value
     * @param write the option's value with another one picked
     * @param <V> the type of the values
     */
    record Choice<T, V>(String key, List<V> values, Function<V, String> valueKey, Function<T, V> read,
            BiFunction<T, V, T> write, Predicate<T> enabledWhen) implements OptionPartV2<T> {
        public Choice {
            values = List.copyOf(values);
        }
    }

    /**
     * A field taking a whole number between two bounds, both inclusive.
     *
     * @param read the number in the value
     * @param write the value with another number
     */
    record WholeNumber<T>(String key, int min, int max, Function<T, Integer> read, BiFunction<T, Integer, T> write,
            Predicate<T> enabledWhen) implements OptionPartV2<T> {
        public WholeNumber {
            if (min > max) {
                throw new IllegalArgumentException("A number part needs min <= max, got " + min + " and " + max);
            }
        }
    }

    /**
     * Several parts shown side by side on one line.
     *
     * @param parts the parts, left to right
     */
    record Row<T>(List<OptionPartV2<T>> parts) implements OptionPartV2<T> {
        public Row {
            parts = List.copyOf(parts);
        }

        @Override
        public String key() {
            return OWN_CAPTION;
        }

        @Override
        public Predicate<T> enabledWhen() {
            return value -> true;
        }
    }

    /** The one part of an on/off option. */
    static OptionPartV2<Boolean> toggle() {
        return new Toggle<>(OWN_CAPTION, value -> value, (value, picked) -> picked, value -> true);
    }

    /** The one part of an option that is one of {@code values}. */
    static <V> OptionPartV2<V> choice(List<V> values, Function<V, String> valueKey) {
        return new Choice<>(OWN_CAPTION, values, valueKey, value -> value, (value, picked) -> picked, value -> true);
    }

    /** The one part of an option that is a whole number between {@code min} and {@code max}, both inclusive. */
    static OptionPartV2<Integer> number(int min, int max) {
        return new WholeNumber<>(OWN_CAPTION, min, max, value -> value, (value, picked) -> picked, value -> true);
    }
}
