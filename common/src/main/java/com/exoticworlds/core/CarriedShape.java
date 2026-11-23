package com.exoticworlds.core;

import java.util.Objects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public final class CarriedShape {
    public static final String WRAPPING_KEY = "wrapping";
    public static final String NO_KEY_PREFIX = "";

    public static final Codec<FlatShape> SHAPE_CODEC = FlatShape.CODEC
            .validate(WorldFolds::verifyFoldable)
            .validate(WorldFolds::verifyGeneratable);

    private final FlatShape shape;
    private final OptionValues options;
    private final WorldFold fold;

    public CarriedShape(FlatShape shape, OptionValues options) {
        this.shape = shape;
        this.options = options;
        this.fold = WorldFolds.of(shape, options);
        WorldFolds.verifyGeneratable(shape).getOrThrow(IllegalArgumentException::new);
    }

    public static MapCodec<CarriedShape> mapCodec(String keyPrefix) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
                SHAPE_CODEC.fieldOf(keyPrefix + WRAPPING_KEY).forGetter(CarriedShape::shape),
                OptionValues.mapCodec(keyPrefix).forGetter(CarriedShape::options)
        ).apply(instance, (FlatShape shape, OptionValues options) -> new CarriedShape(shape, options)));
    }

    public FlatShape shape() {
        return this.shape;
    }

    public OptionValues options() {
        return this.options;
    }

    public WorldFold fold() {
        return this.fold;
    }

    public CarriedShape withShape(FlatShape shape) {
        return new CarriedShape(shape, this.options);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CarriedShape carried
                && this.shape.equals(carried.shape)
                && this.options.equals(carried.options);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.shape, this.options);
    }
}
