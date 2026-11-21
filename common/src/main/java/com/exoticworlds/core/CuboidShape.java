package com.exoticworlds.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record CuboidShape(int aChunks, int bChunks, int cChunks) implements ShapeModel {
    static final String KEY = "cuboid";
    static final String A_CHUNKS_KEY = "a_chunks";
    static final String B_CHUNKS_KEY = "b_chunks";
    static final String C_CHUNKS_KEY = "c_chunks";

    public static final Codec<CuboidShape> CODEC = RecordCodecBuilder.<CuboidShape>create(
            instance -> instance.group(
                    Codec.INT.fieldOf(A_CHUNKS_KEY).forGetter(CuboidShape::aChunks),
                    Codec.INT.fieldOf(B_CHUNKS_KEY).forGetter(CuboidShape::bChunks),
                    Codec.INT.fieldOf(C_CHUNKS_KEY).forGetter(CuboidShape::cChunks)
            ).apply(instance, instance.stable(CuboidShape::new)))
            .fieldOf(KEY)
            .codec();

    public static final StreamCodec<ByteBuf, CuboidShape> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CuboidShape::aChunks,
            ByteBufCodecs.VAR_INT, CuboidShape::bChunks,
            ByteBufCodecs.VAR_INT, CuboidShape::cChunks,
            CuboidShape::new);

    public CuboidAtlas atlas() {
        return CuboidAtlas.of(this.aChunks, this.bChunks, this.cChunks,
                -(this.aChunks / 2) - this.cChunks,
                -(this.bChunks / 2) - this.cChunks);
    }
}
