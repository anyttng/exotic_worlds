package com.exoticworlds.bridge.v2.option;

import com.exoticworlds.api.v2.option.DimensionOption;
import com.exoticworlds.api.v2.option.ShapeOption;
import com.exoticworlds.core.OptionKey;
import com.exoticworlds.core.OptionKeys;
import com.exoticworlds.shape.DeclaredOptions;

public final class OptionBridge {

    public static void register(DimensionOption<?> declared) {
        DeclaredOptions.register(declared);
    }

    public static void register(ShapeOption<?> option) {
        OptionKeys.register(keyOf(option));
    }

    private static <T> OptionKey<T> keyOf(ShapeOption<T> option) {
        return new OptionKey<>(option.key(), option.position(), option.codec(), option.defaultValue());
    }

    private OptionBridge() {
    }
}
