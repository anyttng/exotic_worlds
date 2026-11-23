package com.exoticworlds.api.v2.option;

import java.util.HashMap;
import java.util.Map;

/**
 * The shape options a dimension carries: a value for each {@link ShapeOptionV2} chosen away from its default. An option
 * nothing was chosen for reads its default. Immutable; every wither hands back a new instance.
 */
public final class ShapeOptionsV2 {
    /** No option chosen: every option reads its default. */
    public static final ShapeOptionsV2 DEFAULT = new ShapeOptionsV2(Map.of());

    private final Map<String, Object> chosen;

    private ShapeOptionsV2(Map<String, Object> chosen) {
        this.chosen = chosen;
    }

    /** The value chosen for {@code option}, or its default where none was. */
    @SuppressWarnings("unchecked")
    public <T> T get(ShapeOptionV2<T> option) {
        Object value = this.chosen.get(option.key());
        return value == null ? option.defaultValue() : (T) value;
    }

    /** These options with {@code option} set to {@code value}; a value equal to the default is not kept. */
    public <T> ShapeOptionsV2 with(ShapeOptionV2<T> option, T value) {
        Map<String, Object> grown = new HashMap<>(this.chosen);
        if (value.equals(option.defaultValue())) {
            grown.remove(option.key());
        } else {
            grown.put(option.key(), value);
        }

        return new ShapeOptionsV2(Map.copyOf(grown));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ShapeOptionsV2 options && this.chosen.equals(options.chosen);
    }

    @Override
    public int hashCode() {
        return this.chosen.hashCode();
    }
}
