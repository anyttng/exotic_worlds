package com.exoticworlds.api.v2.option;

import java.util.List;

import com.exoticworlds.bridge.v2.option.OptionBridge;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

/**
 * How a world option is offered to the player: which shapes and which dimensions it is for, and the controls the
 * World Shape Settings screen draws for it. The value itself is a {@link ShapeOptionV2}, registered on its own with
 * {@link ShapeOptionV2#register}; this declaration adds where and how it is shown. Call {@link #register()} at startup,
 * before the registration boundary closes.
 *
 * <p>The option names the shapes and the dimensions, never the other way round. An empty list means all of them: a
 * declaration built by {@link #of} alone is offered for every shape on every dimension. A dimension with no shape
 * stores no options, so nothing is offered there.</p>
 *
 * <p>{@code captionKey} is the translation key of the option's name; the keys of its parts, of a choice's values and
 * of their hints are spelled under it, as {@link OptionPartV2} says.</p>
 *
 * @param <T> the option's value type
 */
public final class DimensionOptionV2<T> {
    private final ShapeOptionV2<T> option;
    private final String captionKey;
    private final List<OptionPartV2<T>> parts;
    private final List<Identifier> shapes;
    private final List<ResourceKey<LevelStem>> dimensions;

    private DimensionOptionV2(ShapeOptionV2<T> option, String captionKey, List<OptionPartV2<T>> parts,
            List<Identifier> shapes, List<ResourceKey<LevelStem>> dimensions) {
        this.option = option;
        this.captionKey = captionKey;
        this.parts = List.copyOf(parts);
        this.shapes = List.copyOf(shapes);
        this.dimensions = List.copyOf(dimensions);
    }

    /** Declares the option for every shape on every dimension, drawn as {@code parts}, top to bottom. */
    public static <T> DimensionOptionV2<T> of(ShapeOptionV2<T> option, String captionKey, List<OptionPartV2<T>> parts) {
        return new DimensionOptionV2<>(option, captionKey, parts, List.of(), List.of());
    }

    /** The same declaration, offered for these shapes alone; see {@code BuiltInShapes} for the mod's own. */
    public DimensionOptionV2<T> forShapes(List<Identifier> offered) {
        return new DimensionOptionV2<>(this.option, this.captionKey, this.parts, offered, this.dimensions);
    }

    /** The same declaration, offered on these dimensions alone. */
    public DimensionOptionV2<T> forDimensions(List<ResourceKey<LevelStem>> offered) {
        return new DimensionOptionV2<>(this.option, this.captionKey, this.parts, this.shapes, offered);
    }

    public ShapeOptionV2<T> option() {
        return this.option;
    }

    public String captionKey() {
        return this.captionKey;
    }

    public List<OptionPartV2<T>> parts() {
        return this.parts;
    }

    /** The shapes the option is offered for; empty means every shape. */
    public List<Identifier> shapes() {
        return this.shapes;
    }

    /** The dimensions the option is offered on; empty means every dimension. */
    public List<ResourceKey<LevelStem>> dimensions() {
        return this.dimensions;
    }

    /** Whether the option is offered where {@code shape} is the shape of {@code dimension}. */
    public boolean offeredFor(Identifier shape, ResourceKey<LevelStem> dimension) {
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
        OptionBridge.register(this);
    }
}
