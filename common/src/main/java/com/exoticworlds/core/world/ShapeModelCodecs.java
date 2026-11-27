package com.exoticworlds.core.world;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.codec.StreamCodec;

final class ShapeModelCodecs {
    private static final int FLAT_ID = 0;
    private static final int CUBOID_ID = 1;

    static final Codec<ShapeModel> CODEC = new ByKey();

    static final StreamCodec<ByteBuf, ShapeModel> STREAM_CODEC = StreamCodec.of(
            ShapeModelCodecs::write,
            ShapeModelCodecs::read);

    private static void write(ByteBuf buffer, ShapeModel shape) {
        switch (shape) {
            case FlatShape flat -> {
                buffer.writeByte(FLAT_ID);
                FlatShape.STREAM_CODEC.encode(buffer, flat);
            }
            case CuboidShape cuboid -> {
                buffer.writeByte(CUBOID_ID);
                CuboidShape.STREAM_CODEC.encode(buffer, cuboid);
            }
        }
    }

    private static ShapeModel read(ByteBuf buffer) {
        int id = buffer.readByte();
        return switch (id) {
            case FLAT_ID -> FlatShape.STREAM_CODEC.decode(buffer);
            case CUBOID_ID -> CuboidShape.STREAM_CODEC.decode(buffer);
            default -> throw new DecoderException("Not a shape model id: " + id);
        };
    }

    private record ByKey() implements Codec<ShapeModel> {
        @Override
        public <T> DataResult<T> encode(ShapeModel input, DynamicOps<T> ops, T prefix) {
            return switch (input) {
                case FlatShape flat -> FlatShape.CODEC.encode(flat, ops, prefix);
                case CuboidShape cuboid -> CuboidShape.CODEC.encode(cuboid, ops, prefix);
            };
        }

        @Override
        public <T> DataResult<Pair<ShapeModel, T>> decode(DynamicOps<T> ops, T input) {
            boolean cuboid = ops.getMap(input).result().map(map -> map.get(CuboidShape.KEY) != null).orElse(false);
            return cuboid
                    ? CuboidShape.CODEC.decode(ops, input).map(read -> read.<ShapeModel>mapFirst(shape -> shape))
                    : FlatShape.CODEC.decode(ops, input).map(read -> read.<ShapeModel>mapFirst(shape -> shape));
        }
    }

    private ShapeModelCodecs() {
    }
}
