package com.exoticworlds.bridge.v1.client;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.client.ShapeCustomizers;
import com.exoticworlds.core.StartupRegistry;

import net.minecraft.resources.Identifier;

public final class CustomizerBridge {
    private static final StartupRegistry<Identifier, ShapeCustomizers.Customizer> CUSTOMIZERS =
            new StartupRegistry<>("Shape customizers");

    public static void register(Identifier shapeId, ShapeCustomizers.Customizer customizer) {
        CUSTOMIZERS.register(shapeId, customizer);
    }

    public static ShapeCustomizers.@Nullable Customizer of(Identifier shapeId) {
        return CUSTOMIZERS.entries().get(shapeId);
    }

    private CustomizerBridge() {
    }
}
