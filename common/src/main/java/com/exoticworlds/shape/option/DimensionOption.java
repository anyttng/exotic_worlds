package com.exoticworlds.shape.option;

import java.util.List;

import com.exoticworlds.core.ShapeOption;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

public final class DimensionOption<T> {
    private final ShapeOption<T> option;
    private final String captionKey;
    private final List<OptionPart<T>> parts;
    private final List<Identifier> shapes;
    private final List<ResourceKey<LevelStem>> dimensions;

    private DimensionOption(ShapeOption<T> option, String captionKey, List<OptionPart<T>> parts,
            List<Identifier> shapes, List<ResourceKey<LevelStem>> dimensions) {
        this.option = option;
        this.captionKey = captionKey;
        this.parts = List.copyOf(parts);
        this.shapes = List.copyOf(shapes);
        this.dimensions = List.copyOf(dimensions);
    }

    public static <T> DimensionOption<T> of(ShapeOption<T> option, String captionKey, List<OptionPart<T>> parts) {
        return new DimensionOption<>(option, captionKey, parts, List.of(), List.of());
    }

    public DimensionOption<T> forShapes(List<Identifier> offered) {
        return new DimensionOption<>(this.option, this.captionKey, this.parts, offered, this.dimensions);
    }

    public DimensionOption<T> forDimensions(List<ResourceKey<LevelStem>> offered) {
        return new DimensionOption<>(this.option, this.captionKey, this.parts, this.shapes, offered);
    }

    public ShapeOption<T> option() {
        return this.option;
    }

    public String captionKey() {
        return this.captionKey;
    }

    public List<OptionPart<T>> parts() {
        return this.parts;
    }

    public List<Identifier> shapes() {
        return this.shapes;
    }

    public boolean offeredFor(Identifier shape, ResourceKey<LevelStem> dimension) {
        return (this.shapes.isEmpty() || this.shapes.contains(shape))
                && (this.dimensions.isEmpty() || this.dimensions.contains(dimension));
    }
}
