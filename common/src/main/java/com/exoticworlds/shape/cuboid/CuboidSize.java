package com.exoticworlds.shape.cuboid;

import com.exoticworlds.core.world.CuboidShape;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record CuboidSize(int aChunks, int bChunks, int cChunks) {
    static final Codec<CuboidSize> CODEC = RecordCodecBuilder.create(size -> size.group(
            Codec.INT.fieldOf(CuboidShape.A_CHUNKS_KEY).forGetter(CuboidSize::aChunks),
            Codec.INT.fieldOf(CuboidShape.B_CHUNKS_KEY).forGetter(CuboidSize::bChunks),
            Codec.INT.fieldOf(CuboidShape.C_CHUNKS_KEY).forGetter(CuboidSize::cChunks)).apply(size, CuboidSize::new));

    static CuboidSize cube(int edgeChunks) {
        return new CuboidSize(edgeChunks, edgeChunks, edgeChunks);
    }

    static CuboidSize of(CuboidShape shape) {
        return new CuboidSize(shape.aChunks(), shape.bChunks(), shape.cChunks());
    }

    CuboidShape toShape() {
        return new CuboidShape(this.aChunks, this.bChunks, this.cChunks);
    }
}
