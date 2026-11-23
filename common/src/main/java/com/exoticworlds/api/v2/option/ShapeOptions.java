package com.exoticworlds.api.v2.option;

import java.util.HashMap;
import java.util.Map;

/**
 * The shape options a dimension carries: a value for each {@link ShapeOption} chosen away from its default. An option
 * nothing was chosen for reads its default. Immutable; every wither hands back a new instance.
 */
public final class ShapeOptions {
    /** No option chosen: every option reads its default. */
    public static final ShapeOptions DEFAULT = new ShapeOptions(Map.of());

    private final Map<String, Object> chosen;

    private ShapeOptions(Map<String, Object> chosen) {
        this.chosen = chosen;
    }

    /** The value chosen for {@code option}, or its default where none was. */
    @SuppressWarnings("unchecked")
    public <T> T get(ShapeOption<T> option) {
        Object value = this.chosen.get(option.key());
        return value == null ? option.defaultValue() : (T) value;
    }

    /** These options with {@code option} set to {@code value}; a value equal to the default is not kept. */
    public <T> ShapeOptions with(ShapeOption<T> option, T value) {
        Map<String, Object> grown = new HashMap<>(this.chosen);
        if (value.equals(option.defaultValue())) {
            grown.remove(option.key());
        } else {
            grown.put(option.key(), value);
        }

        return new ShapeOptions(Map.copyOf(grown));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ShapeOptions options && this.chosen.equals(options.chosen);
    }

    @Override
    public int hashCode() {
        return this.chosen.hashCode();
    }
}
