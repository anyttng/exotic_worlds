package com.exoticworlds.api.v2.option;

import java.util.List;

import com.exoticworlds.api.v1.option.WorldOption;
import com.exoticworlds.api.v1.option.WorldOptions;
import com.exoticworlds.shape.DeclaredOptions;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

/**
 * How a world option is offered to the player: which shapes and which dimensions it is for, and the controls the
 * World Shape Settings screen draws for it. The value itself is a {@link WorldOption}, registered on its own with
 * {@link WorldOptions#register}; this declaration adds where and how it is shown. Call {@link #register()} at startup,
 * before the registration boundary closes.
 *
 * <p>The option names the shapes and the dimensions, never the other way round. An empty list means all of them: a
 * declaration built by {@link #of} alone is offered for every shape on every dimension. A dimension with no shape
 * stores no options, so nothing is offered there.</p>
 *
 * <p>{@code captionKey} is the translation key of the option's name; the keys of its parts, of a choice's values and
 * of their hints are spelled under it, as {@link OptionPart} says.</p>
 *
 * @param <T> the option's value type
 */
public final class DimensionOption<T> {
    private final WorldOption<T> option;
    private final String captionKey;
    private final List<OptionPart<T>> parts;
    private final List<ResourceLocation> shapes;
    private final List<ResourceKey<LevelStem>> dimensions;

    private DimensionOption(WorldOption<T> option, String captionKey, List<OptionPart<T>> parts,
            List<ResourceLocation> shapes, List<ResourceKey<LevelStem>> dimensions) {
        this.option = option;
        this.captionKey = captionKey;
        this.parts = List.copyOf(parts);
        this.shapes = List.copyOf(shapes);
        this.dimensions = List.copyOf(dimensions);
    }

    /** Declares the option for every shape on every dimension, drawn as {@code parts}, top to bottom. */
    public static <T> DimensionOption<T> of(WorldOption<T> option, String captionKey, List<OptionPart<T>> parts) {
        return new DimensionOption<>(option, captionKey, parts, List.of(), List.of());
    }

    /** The same declaration, offered for these shapes alone; see {@code BuiltInShapes} for the mod's own. */
    public DimensionOption<T> forShapes(List<ResourceLocation> offered) {
        return new DimensionOption<>(this.option, this.captionKey, this.parts, offered, this.dimensions);
    }

    /** The same declaration, offered on these dimensions alone. */
    public DimensionOption<T> forDimensions(List<ResourceKey<LevelStem>> offered) {
        return new DimensionOption<>(this.option, this.captionKey, this.parts, this.shapes, offered);
    }

    public WorldOption<T> option() {
        return this.option;
    }

    public String captionKey() {
        return this.captionKey;
    }

    public List<OptionPart<T>> parts() {
        return this.parts;
    }

    /** The shapes the option is offered for; empty means every shape. */
    public List<ResourceLocation> shapes() {
        return this.shapes;
    }

    /** Whether the option is offered where {@code shape} is the shape of {@code dimension}. */
    public boolean offeredFor(ResourceLocation shape, ResourceKey<LevelStem> dimension) {
        return (this.shapes.isEmpty() || this.shapes.contains(shape))
                && (this.dimensions.isEmpty() || this.dimensions.contains(dimension));
    }

    /**
     * Registers the declaration.
     *
     * @throws IllegalStateException if the registration boundary has already closed, or a declaration is already
     *         registered for this option
     */
    public void register() {
        DeclaredOptions.register(this);
    }
}
