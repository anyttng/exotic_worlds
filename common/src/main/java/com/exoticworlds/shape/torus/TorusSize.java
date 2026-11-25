package com.exoticworlds.shape.torus;

import com.exoticworlds.core.world.FlatShape;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record TorusSize(int xWidthChunks, int zWidthChunks, int shiftChunks) {
    private static final String X_WIDTH_KEY = "x_width_chunks";
    private static final String Z_WIDTH_KEY = "z_width_chunks";
    private static final String SHIFT_KEY = "shift_chunks";

    static final Codec<TorusSize> CODEC = RecordCodecBuilder.create(size -> size.group(
            Codec.INT.fieldOf(X_WIDTH_KEY).forGetter(TorusSize::xWidthChunks),
            Codec.INT.fieldOf(Z_WIDTH_KEY).forGetter(TorusSize::zWidthChunks),
            Codec.INT.fieldOf(SHIFT_KEY).forGetter(TorusSize::shiftChunks)).apply(size, TorusSize::new));

    static TorusSize square(int widthChunks) {
        return new TorusSize(widthChunks, widthChunks, FlatShape.NO_SKEW);
    }
}
