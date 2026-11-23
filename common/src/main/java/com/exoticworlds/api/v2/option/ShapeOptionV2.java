package com.exoticworlds.api.v2.option;

import com.exoticworlds.bridge.v2.option.OptionBridge;
import com.mojang.serialization.Codec;

/**
 * One option of a world's shape: the key its value is stored under, its place among the options on the World Shape
 * Settings screen, the codec of its value and the value a world takes where nothing was chosen. Call
 * {@link #register()} at startup, before the registration boundary closes, so a world stores the option's value. How
 * the option is offered to the player is a {@link DimensionOptionV2}.
 *
 * @param key the key the value is stored under, unique across every mod
 * @param position the option's place among the others, lowest first
 * @param codec the codec of the value
 * @param defaultValue the value a world takes where nothing was chosen; it is never stored
 * @param <T> the option's value type
 */
public record ShapeOptionV2<T>(String key, int position, Codec<T> codec, T defaultValue) {

    /**
     * Registers the option, so a world stores its value.
     *
     * @throws IllegalStateException if the registration boundary has already closed, or an option is already
     *         registered under this key
     */
    public void register() {
        OptionBridge.register(this);
    }
}
