package com.exoticworlds.api.v2.shape;

import com.exoticworlds.api.v2.option.ShapeOptions;
import com.exoticworlds.bridge.v2.shape.ShapeBridge;

import net.minecraft.resources.Identifier;

/**
 * A shape another mod adds to the World Shape Settings screen. It is offered on every dimension, has no screen and no
 * settings of its own: what the player tunes are options, and an option names the shapes it is for
 * ({@code DimensionOption.forShapes}). Call {@link #register()} at startup, before the registration boundary closes.
 *
 * <p>The shape's name is the translation key {@code gui.<namespace>.world_shape.<path>} of its id, and its one-line
 * description that key plus {@code .hint}.</p>
 *
 * <p>A world stores the spans and the options, never the shape. A dimension of a world being re-created shows the
 * shape again where {@link #spans} gives, for the stored options, exactly the stored spans; shapes are asked in the
 * order they were registered.</p>
 */
public interface DimensionShape {
    Identifier id();

    /**
     * The spans of one dimension the shape is chosen for. Spans that loop no axis leave the dimension as the game
     * makes it; a looped axis outside the sizes the dimension allows holds the screen's Done.
     */
    LoopSpans spans(DimensionContext context);

    /** Whether the shape takes a size preset. While the Overworld is on one that does not, the preset row is off. */
    default boolean takesPresets() {
        return false;
    }

    /**
     * The Overworld's options with a size preset of {@code chunkWidth} chunks applied, asked only where
     * {@link #takesPresets()} answers true. A preset draws selected while this changes nothing.
     */
    default ShapeOptions withPreset(int chunkWidth, ShapeOptions options) {
        return options;
    }

    /**
     * Registers the shape.
     *
     * @throws IllegalStateException if the registration boundary has already closed, or a shape is already registered
     *         under this id
     */
    default void register() {
        ShapeBridge.register(this);
    }
}
