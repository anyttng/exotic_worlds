package com.exoticworlds.core.world;

import java.util.Objects;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.core.option.ShapeOptions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;

public final class CarriedShape {
    public static final String WRAPPING_KEY = "wrapping";
    public static final String ID_KEY = "id";
    public static final String NO_KEY_PREFIX = "";

    public static final Codec<FlatShape> SHAPE_CODEC = FlatShape.CODEC
            .validate(WorldFolds::verifyFoldable)
            .validate(WorldFolds::verifyGeneratable);

    public static final Codec<CarriedShape> CODEC = mapCodec(NO_KEY_PREFIX).codec();

    private final @Nullable Identifier shapeId;
    private final FlatShape shape;
    private final ShapeOptions options;
    private final WorldFold fold;

    public CarriedShape(@Nullable Identifier shapeId, FlatShape shape, ShapeOptions options) {
        this.shapeId = shapeId;
        this.shape = shape;
        this.options = options;
        this.fold = WorldFolds.of(shape, options);
        WorldFolds.verifyGeneratable(shape).getOrThrow(IllegalArgumentException::new);
    }

    public static MapCodec<CarriedShape> mapCodec(String keyPrefix) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
                Identifier.CODEC.optionalFieldOf(keyPrefix + ID_KEY).forGetter(CarriedShape::getShapeId),
                SHAPE_CODEC.fieldOf(keyPrefix + WRAPPING_KEY).forGetter(CarriedShape::getShape),
                ShapeOptions.mapCodec(keyPrefix).forGetter(CarriedShape::getOptions)
        ).apply(instance, (Optional<Identifier> shapeId, FlatShape shape, ShapeOptions options) ->
                new CarriedShape(shapeId.orElse(null), shape, options)));
    }

    public Optional<Identifier> getShapeId() {
        return Optional.ofNullable(this.shapeId);
    }

    public Optional<Identifier> findShapeId() {
        return this.shapeId != null ? Optional.of(this.shapeId) : LegacyShapeIds.recognize(this.shape);
    }

    public FlatShape getShape() {
        return this.shape;
    }

    public ShapeOptions getOptions() {
        return this.options;
    }

    public WorldFold getFold() {
        return this.fold;
    }

    public CarriedShape withShape(FlatShape shape) {
        return new CarriedShape(this.shapeId, shape, this.options);
    }

    public CarriedShape withOptions(ShapeOptions options) {
        return new CarriedShape(this.shapeId, this.shape, options);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CarriedShape carried
                && Objects.equals(this.shapeId, carried.shapeId)
                && this.shape.equals(carried.shape)
                && this.options.equals(carried.options);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.shapeId, this.shape, this.options);
    }
}
