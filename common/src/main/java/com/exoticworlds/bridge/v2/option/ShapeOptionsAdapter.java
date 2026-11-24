package com.exoticworlds.bridge.v2.option;

import com.exoticworlds.api.v2.option.ShapeOptionV2;
import com.exoticworlds.api.v2.option.ShapeOptionsV2;
import com.exoticworlds.core.option.ShapeOption;
import com.exoticworlds.core.option.ShapeOptionRegistry;
import com.exoticworlds.core.option.ShapeOptions;

public final class ShapeOptionsAdapter {

    public static ShapeOptionsV2 toV2(ShapeOptions stored) {
        ShapeOptionsV2 converted = ShapeOptionsV2.DEFAULT;
        for (ShapeOption<?> key : ShapeOptionRegistry.getAll()) {
            converted = carried(converted, key, stored);
        }

        return converted;
    }

    public static ShapeOptions fromV2(ShapeOptionsV2 chosen) {
        ShapeOptions converted = ShapeOptions.DEFAULT;
        for (ShapeOption<?> key : ShapeOptionRegistry.getAll()) {
            converted = carried(converted, key, chosen);
        }

        return converted;
    }

    public static <T> ShapeOption<T> fromV2(ShapeOptionV2<T> option) {
        return new ShapeOption<>(option.key(), option.position(), option.codec(), option.defaultValue());
    }

    private static <T> ShapeOptionsV2 carried(ShapeOptionsV2 into, ShapeOption<T> key, ShapeOptions stored) {
        return into.with(toV2(key), stored.get(key));
    }

    private static <T> ShapeOptions carried(ShapeOptions into, ShapeOption<T> key, ShapeOptionsV2 chosen) {
        return into.with(key, chosen.get(toV2(key)));
    }

    public static <T> ShapeOptionV2<T> toV2(ShapeOption<T> key) {
        return new ShapeOptionV2<>(key.key(), key.position(), key.codec(), key.defaultValue());
    }

    private ShapeOptionsAdapter() {
    }
}
