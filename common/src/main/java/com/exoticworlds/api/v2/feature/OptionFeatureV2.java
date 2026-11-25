package com.exoticworlds.api.v2.feature;

import java.util.List;

import com.mojang.serialization.Codec;

import net.minecraft.resources.Identifier;

/**
 * A feature the player sets through one option on the World Shape Settings screen. The feature owns the option
 * whole: how it is saved, what a world reads when it never stated it, what a new world starts with, and the controls
 * that edit it. Inside a moment the value comes typed from {@link MomentV2#getValue}.
 *
 * @param <T> the option's value type; an immutable value with {@code equals}
 */
public abstract class OptionFeatureV2<T> extends FeatureV2 {
    private final Codec<T> codec;
    private final T storedDefault;
    private final T newWorldValue;
    private final String captionKey;
    private final List<OptionPartV2<T>> parts;

    /**
     * @param id the feature's id, also the key the value is saved under
     * @param codec how the value is saved
     * @param storedDefault what a world reads when it never stated the option; a value equal to it is not written
     * @param newWorldValue what the screen starts a new world with; may differ from {@code storedDefault}
     * @param captionKey the translation key of the option's caption; a part's own key and a choice's value keys are
     *        appended to it with a dot
     * @param parts the controls that edit the value, in the order they are shown
     */
    protected OptionFeatureV2(Identifier id, Codec<T> codec, T storedDefault, T newWorldValue, String captionKey,
            List<OptionPartV2<T>> parts) {
        super(id);
        this.codec = codec;
        this.storedDefault = storedDefault;
        this.newWorldValue = newWorldValue;
        this.captionKey = captionKey;
        this.parts = List.copyOf(parts);
    }

    /**
     * Whether the feature acts at this value. A feature that is off has none of its moments called. Every value is on
     * unless this is overridden.
     */
    public boolean isOn(T value) {
        return true;
    }

    /** How the value is saved. */
    public final Codec<T> getCodec() {
        return this.codec;
    }

    /** What a world reads when it never stated the option. */
    public final T getStoredDefault() {
        return this.storedDefault;
    }

    /** What the screen starts a new world with. */
    public final T getNewWorldValue() {
        return this.newWorldValue;
    }

    /** The translation key of the option's caption. */
    public final String getCaptionKey() {
        return this.captionKey;
    }

    /** The controls that edit the value, in the order they are shown. */
    public final List<OptionPartV2<T>> getParts() {
        return this.parts;
    }
}
