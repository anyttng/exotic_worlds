package com.exoticworlds.api.v2.shape;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.exoticworlds.api.v2.feature.OptionPartV2;
import com.mojang.serialization.Codec;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

/**
 * One value a shape is tuned with, a width for example. The world stores it per dimension under the parameter's id,
 * the shape's page draws it from its parts, and the shape reads it with {@link ShapeValuesV2#getValue}.
 *
 * <p>Build one with {@link #of} and keep it in a constant; the shape returns it from
 * {@link ShapeV2#getParameters()}.</p>
 *
 * @param <T> the type of the value
 */
public final class ParameterV2<T> {
    private final Identifier id;
    private final Codec<T> codec;
    private final T storedDefault;
    private final String captionKey;
    private final List<OptionPartV2<T>> parts;
    private final Function<ResourceKey<LevelStem>, T> newWorldValue;
    private final BiFunction<T, Integer, T> presetWidth;
    private final boolean isTakingPresets;

    private ParameterV2(Identifier id, Codec<T> codec, T storedDefault, String captionKey, List<OptionPartV2<T>> parts,
            Function<ResourceKey<LevelStem>, T> newWorldValue, BiFunction<T, Integer, T> presetWidth,
            boolean isTakingPresets) {
        this.id = id;
        this.codec = codec;
        this.storedDefault = storedDefault;
        this.captionKey = captionKey;
        this.parts = List.copyOf(parts);
        this.newWorldValue = newWorldValue;
        this.presetWidth = presetWidth;
        this.isTakingPresets = isTakingPresets;
    }

    /**
     * A parameter that takes no size preset and starts every new dimension at {@code storedDefault}.
     *
     * @param id the id the value is saved under, in your mod's namespace
     * @param codec how the value is saved
     * @param storedDefault the value of a dimension that never stated one; it is not written to the save
     * @param captionKey the translation key of the caption on the shape's page
     * @param parts the controls the value is drawn with, top to bottom
     */
    public static <T> ParameterV2<T> of(Identifier id, Codec<T> codec, T storedDefault, String captionKey,
            List<OptionPartV2<T>> parts) {
        return new ParameterV2<>(id, codec, storedDefault, captionKey, parts, dimension -> storedDefault,
                (value, widthChunks) -> value, false);
    }

    /** This parameter starting a new world at {@code value} of the dimension, a smaller width in the Nether for one. */
    public ParameterV2<T> withNewWorldValue(Function<ResourceKey<LevelStem>, T> value) {
        return new ParameterV2<>(this.id, this.codec, this.storedDefault, this.captionKey, this.parts, value,
                this.presetWidth, this.isTakingPresets);
    }

    /**
     * This parameter taking the size presets of the screen: {@code write} gives the value with a preset width, in
     * chunks, applied. A shape with at least one such parameter is offered the preset row.
     */
    public ParameterV2<T> withPresetWidth(BiFunction<T, Integer, T> write) {
        return new ParameterV2<>(this.id, this.codec, this.storedDefault, this.captionKey, this.parts,
                this.newWorldValue, write, true);
    }

    /** The id the value is saved under. */
    public Identifier getId() {
        return this.id;
    }

    /** How the value is saved. */
    public Codec<T> getCodec() {
        return this.codec;
    }

    /** The value of a dimension that never stated one. */
    public T getStoredDefault() {
        return this.storedDefault;
    }

    /** The translation key of the caption on the shape's page. */
    public String getCaptionKey() {
        return this.captionKey;
    }

    /** The controls the value is drawn with. */
    public List<OptionPartV2<T>> getParts() {
        return this.parts;
    }

    /** The value a new world starts {@code dimension} at. */
    public T newWorldValue(ResourceKey<LevelStem> dimension) {
        return this.newWorldValue.apply(dimension);
    }

    /** Whether the parameter takes a size preset. */
    public boolean isTakingPresets() {
        return this.isTakingPresets;
    }

    /** {@code value} with a preset width of {@code widthChunks} chunks applied; unchanged without a preset rule. */
    public T withPresetWidth(T value, int widthChunks) {
        return this.presetWidth.apply(value, widthChunks);
    }
}
