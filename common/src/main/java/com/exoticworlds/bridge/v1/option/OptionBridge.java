package com.exoticworlds.bridge.v1.option;

import java.util.List;

import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.api.v1.option.WorldOption;
import com.exoticworlds.core.OptionKey;
import com.exoticworlds.core.OptionKeys;
import com.exoticworlds.core.OptionValues;

public final class OptionBridge {

    public static <T> WorldOption<T> register(WorldOption<T> option) {
        OptionKeys.register(keyOf(option));
        return option;
    }

    public static List<WorldOption<?>> all() {
        return OptionKeys.all().stream().<WorldOption<?>>map(OptionBridge::twin).toList();
    }

    public static GenerationOptions toV1(OptionValues values) {
        GenerationOptions converted = GenerationOptions.DEFAULT;
        for (OptionKey<?> key : OptionKeys.all()) {
            converted = carried(converted, key, values);
        }

        return converted;
    }

    public static OptionValues fromV1(GenerationOptions options) {
        OptionValues converted = OptionValues.DEFAULT;
        for (OptionKey<?> key : OptionKeys.all()) {
            converted = carried(converted, key, options);
        }

        return converted;
    }

    public static <T> WorldOption<T> twin(OptionKey<T> key) {
        return new WorldOption<>(key.key(), key.position(), key.codec(), key.defaultValue());
    }

    private static <T> GenerationOptions carried(GenerationOptions into, OptionKey<T> key, OptionValues values) {
        return into.with(twin(key), values.get(key));
    }

    private static <T> OptionValues carried(OptionValues into, OptionKey<T> key, GenerationOptions options) {
        return into.with(key, options.get(twin(key)));
    }

    private static <T> OptionKey<T> keyOf(WorldOption<T> option) {
        return new OptionKey<>(option.key(), option.position(), option.codec(), option.defaultValue());
    }

    private OptionBridge() {
    }
}
