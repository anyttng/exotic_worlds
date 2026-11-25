package com.exoticworlds.api.v1.client;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.bridge.v1.client.CustomizerBridge;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;

/**
 * The screen behind the Customize button on the create-world screen, one per shape. A shape with no customizer
 * registered leaves the button dark; register at startup, from the client side only, beside
 * {@link com.exoticworlds.api.v1.shape.ShapeModule#register()}.
 *
 * @deprecated no screen shows a Customize button any more, so a customizer registered here is never opened. A
 *     {@link com.exoticworlds.api.v2.shape.ShapeV2} has no screen of its own: what the player tunes are
 *     options, declared as a {@link com.exoticworlds.api.v2.feature.OptionFeatureV2}.
 */
@Deprecated
public final class ShapeCustomizers {

    /** Builds the settings screen for one shape. {@code parent} is the screen to return to when it closes. */
    @FunctionalInterface
    public interface Customizer {
        Screen createScreen(Screen parent);
    }

    /**
     * Registers the customizer for the shape declared under {@code shapeId}.
     *
     * @throws IllegalStateException if the registration boundary has already closed, or {@code shapeId} already has
     *         a customizer
     */
    public static void register(Identifier shapeId, Customizer customizer) {
        CustomizerBridge.register(shapeId, customizer);
    }

    /** The customizer registered for {@code shapeId}, or {@code null} where that shape offers no settings screen. */
    public static @Nullable Customizer of(Identifier shapeId) {
        return CustomizerBridge.of(shapeId);
    }

    private ShapeCustomizers() {
    }
}
