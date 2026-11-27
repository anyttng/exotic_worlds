package com.exoticworlds.core.world;

import java.util.Optional;
import java.util.function.Function;

import com.exoticworlds.core.registry.StartupRegistry;

import net.minecraft.resources.Identifier;

public final class LegacyShapeIds {
    private static final String KEY = "legacy_shape_ids";

    private static final StartupRegistry<String, Function<FlatShape, Optional<Identifier>>> READERS =
            new StartupRegistry<>("Legacy shape ids");

    public static void register(Function<FlatShape, Optional<Identifier>> reader) {
        READERS.register(KEY, reader);
    }

    public static Optional<Identifier> recognize(FlatShape shape) {
        Function<FlatShape, Optional<Identifier>> reader = READERS.getEntries().get(KEY);
        return reader == null ? Optional.empty() : reader.apply(shape);
    }

    private LegacyShapeIds() {
    }
}
