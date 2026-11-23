package com.exoticworlds.core;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

public final class ShapeOptions {
    public static final ShapeOptions DEFAULT = new ShapeOptions(Map.of());

    private final Map<String, Object> chosen;

    private ShapeOptions(Map<String, Object> chosen) {
        this.chosen = chosen;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(ShapeOption<T> key) {
        Object value = this.chosen.get(key.key());
        return value == null ? key.defaultValue() : (T) value;
    }

    public <T> ShapeOptions with(ShapeOption<T> key, T value) {
        Map<String, Object> grown = new HashMap<>(this.chosen);
        if (value.equals(key.defaultValue())) {
            grown.remove(key.key());
        } else {
            grown.put(key.key(), value);
        }

        return new ShapeOptions(Map.copyOf(grown));
    }

    public static MapCodec<ShapeOptions> mapCodec(String keyPrefix) {
        return new Assembled(keyPrefix);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ShapeOptions values && this.chosen.equals(values.chosen);
    }

    @Override
    public int hashCode() {
        return this.chosen.hashCode();
    }

    private static final class Assembled extends MapCodec<ShapeOptions> {
        private final String keyPrefix;

        private Assembled(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }

        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return ShapeOptionRegistry.all().stream().map(key -> ops.createString(this.keyPrefix + key.key()));
        }

        @Override
        public <T> DataResult<ShapeOptions> decode(DynamicOps<T> ops, MapLike<T> input) {
            DataResult<ShapeOptions> decoded = DataResult.success(DEFAULT);
            for (ShapeOption<?> key : ShapeOptionRegistry.all()) {
                decoded = decoded.flatMap(values -> read(values, key, this.keyPrefix, ops, input));
            }

            return decoded;
        }

        @Override
        public <T> RecordBuilder<T> encode(ShapeOptions input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            RecordBuilder<T> builder = prefix;
            for (ShapeOption<?> key : ShapeOptionRegistry.all()) {
                builder = write(input, key, this.keyPrefix, ops, builder);
            }

            return builder;
        }

        private static <T, V> DataResult<ShapeOptions> read(ShapeOptions into, ShapeOption<V> key, String keyPrefix,
                DynamicOps<T> ops, MapLike<T> input) {
            T value = input.get(keyPrefix + key.key());
            return value == null
                    ? DataResult.success(into)
                    : key.codec().parse(ops, value).map(parsed -> into.with(key, parsed));
        }

        private static <T, V> RecordBuilder<T> write(ShapeOptions from, ShapeOption<V> key, String keyPrefix,
                DynamicOps<T> ops, RecordBuilder<T> builder) {
            V value = from.get(key);
            return value.equals(key.defaultValue())
                    ? builder
                    : builder.add(keyPrefix + key.key(), key.codec().encodeStart(ops, value));
        }
    }
}
