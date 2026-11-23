package com.exoticworlds.bridge.v1.option;

import java.util.List;

import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.api.v1.option.WorldOption;
import com.exoticworlds.core.ShapeOption;
import com.exoticworlds.core.ShapeOptionRegistry;
import com.exoticworlds.core.ShapeOptions;

public final class OptionBridge {

    public static <T> WorldOption<T> register(WorldOption<T> option) {
        ShapeOptionRegistry.register(keyOf(option));
        return option;
    }

    public static List<WorldOption<?>> all() {
        return ShapeOptionRegistry.all().stream().<WorldOption<?>>map(OptionBridge::twin).toList();
    }

    public static GenerationOptions toV1(ShapeOptions values) {
        GenerationOptions converted = GenerationOptions.DEFAULT;
        for (ShapeOption<?> key : ShapeOptionRegistry.all()) {
            converted = carried(converted, key, values);
        }

        return converted;
    }

    public static ShapeOptions fromV1(GenerationOptions options) {
        ShapeOptions converted = ShapeOptions.DEFAULT;
        for (ShapeOption<?> key : ShapeOptionRegistry.all()) {
            converted = carried(converted, key, options);
        }

        return converted;
    }

    public static <T> WorldOption<T> twin(ShapeOption<T> key) {
        return new WorldOption<>(key.key(), key.position(), key.codec(), key.defaultValue());
    }

    private static <T> GenerationOptions carried(GenerationOptions into, ShapeOption<T> key, ShapeOptions values) {
        return into.with(twin(key), values.get(key));
    }

    private static <T> ShapeOptions carried(ShapeOptions into, ShapeOption<T> key, GenerationOptions options) {
        return into.with(key, options.get(twin(key)));
    }

    private static <T> ShapeOption<T> keyOf(WorldOption<T> option) {
        return new ShapeOption<>(option.key(), option.position(), option.codec(), option.defaultValue());
    }

    private OptionBridge() {
    }
}
