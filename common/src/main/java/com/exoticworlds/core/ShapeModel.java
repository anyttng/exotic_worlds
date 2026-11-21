package com.exoticworlds.core;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

public sealed interface ShapeModel permits FlatShape, CuboidShape {
    Codec<ShapeModel> CODEC = ShapeModelCodecs.CODEC;

    StreamCodec<ByteBuf, ShapeModel> STREAM_CODEC = ShapeModelCodecs.STREAM_CODEC;
}
