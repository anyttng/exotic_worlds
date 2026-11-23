package com.exoticworlds.shape;

import com.exoticworlds.api.v2.option.ShapeOption;
import com.exoticworlds.api.v2.option.ShapeOptions;
import com.exoticworlds.core.OptionKey;
import com.exoticworlds.core.OptionKeys;
import com.exoticworlds.core.OptionValues;

public final class ShapeOptionsAdapter {

    public static ShapeOptions shapeOptions(OptionValues stored) {
        ShapeOptions converted = ShapeOptions.DEFAULT;
        for (OptionKey<?> key : OptionKeys.all()) {
            converted = carried(converted, key, stored);
        }

        return converted;
    }

    public static OptionValues optionValues(ShapeOptions chosen) {
        OptionValues converted = OptionValues.DEFAULT;
        for (OptionKey<?> key : OptionKeys.all()) {
            converted = carried(converted, key, chosen);
        }

        return converted;
    }

    public static <T> OptionKey<T> optionKey(ShapeOption<T> option) {
        return new OptionKey<>(option.key(), option.position(), option.codec(), option.defaultValue());
    }

    private static <T> ShapeOptions carried(ShapeOptions into, OptionKey<T> key, OptionValues stored) {
        return into.with(shapeOption(key), stored.get(key));
    }

    private static <T> OptionValues carried(OptionValues into, OptionKey<T> key, ShapeOptions chosen) {
        return into.with(key, chosen.get(shapeOption(key)));
    }

    public static <T> ShapeOption<T> shapeOption(OptionKey<T> key) {
        return new ShapeOption<>(key.key(), key.position(), key.codec(), key.defaultValue());
    }

    private ShapeOptionsAdapter() {
    }
}
