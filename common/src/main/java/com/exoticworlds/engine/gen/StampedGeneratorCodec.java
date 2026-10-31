package com.exoticworlds.engine.gen;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.ExoticWorlds;
import com.exoticworlds.accessors.ShapeStamp;
import com.exoticworlds.core.CarriedShape;
import com.exoticworlds.core.DimensionTable;
import com.exoticworlds.core.ShapedChunkGenerator;
import com.exoticworlds.migration.FormerNamespace;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import net.minecraft.world.level.chunk.ChunkGenerator;

public final class StampedGeneratorCodec {
    private static final String KEY_PREFIX = ExoticWorlds.MODID + ":";

    static final String SHAPE_KEY = KEY_PREFIX + CarriedShape.WRAPPING_KEY;

    private static final MapCodec<CarriedShape> CARRIED_CODEC = CarriedShape.mapCodec(KEY_PREFIX);

    private static final String FORMER_SHAPE_KEY = FormerNamespace.formerKey(SHAPE_KEY);

    private static final MapCodec<CarriedShape> FORMER_CARRIED_CODEC = CarriedShape.mapCodec(FormerNamespace.KEY_PREFIX);

    static final String TABLE_KEY = KEY_PREFIX + DimensionTable.KEY;

    private static final MapCodec<DimensionTable> TABLE_CODEC = DimensionTable.CODEC.fieldOf(TABLE_KEY);

    public static Codec<ChunkGenerator> over(Codec<ChunkGenerator> dispatch) {
        return new StampCarrying(dispatch);
    }

    private static @Nullable ShapeStamp stampOf(ChunkGenerator generator) {
        if (generator instanceof ShapedChunkGenerator) {
            return null;
        }

        return tableStampOf(generator);
    }

    private static @Nullable ShapeStamp tableStampOf(ChunkGenerator generator) {
        return generator instanceof ShapeStamp stamp ? stamp : null;
    }

    private record StampCarrying(Codec<ChunkGenerator> dispatch) implements Codec<ChunkGenerator> {
        @Override
        public <T> DataResult<T> encode(ChunkGenerator input, DynamicOps<T> ops, T prefix) {
            DataResult<T> encoded = this.dispatch.encode(input, ops, prefix);
            ShapeStamp stamp = stampOf(input);
            CarriedShape carried = stamp == null ? null : stamp.exotic_worlds$carriedShape();
            ShapeStamp tableStamp = tableStampOf(input);
            DimensionTable table = tableStamp == null ? DimensionTable.EMPTY : tableStamp.exotic_worlds$dimensionTable();
            if (carried == null && table.isEmpty()) {
                return encoded;
            }

            return encoded.flatMap(map -> {
                RecordBuilder<T> builder = ops.mapBuilder();
                if (carried != null) {
                    builder = CARRIED_CODEC.encode(carried, ops, builder);
                }

                if (!table.isEmpty()) {
                    builder = TABLE_CODEC.encode(table, ops, builder);
                }

                return builder.build(map);
            });
        }

        @Override
        public <T> DataResult<Pair<ChunkGenerator, T>> decode(DynamicOps<T> ops, T input) {
            return this.dispatch.decode(ops, input)
                    .flatMap(decoded -> fillStamp(ops, input, decoded))
                    .flatMap(decoded -> fillTable(ops, input, decoded));
        }

        private static <T> DataResult<Pair<ChunkGenerator, T>> fillStamp(DynamicOps<T> ops, T input,
                Pair<ChunkGenerator, T> decoded) {
            ShapeStamp stamp = stampOf(decoded.getFirst());
            MapLike<T> map = ops.getMap(input).result().orElse(null);
            MapCodec<CarriedShape> codec = map == null ? null : stampCodecOf(map);
            if (stamp == null || codec == null) {
                return DataResult.success(decoded);
            }

            return codec.decode(ops, map).map(carried -> {
                stamp.exotic_worlds$stamp(carried);
                return decoded;
            });
        }

        private static <T> DataResult<Pair<ChunkGenerator, T>> fillTable(DynamicOps<T> ops, T input,
                Pair<ChunkGenerator, T> decoded) {
            ShapeStamp stamp = tableStampOf(decoded.getFirst());
            MapLike<T> map = ops.getMap(input).result().orElse(null);
            if (stamp == null || map == null || map.get(TABLE_KEY) == null) {
                return DataResult.success(decoded);
            }

            return TABLE_CODEC.decode(ops, map).map(table -> {
                stamp.exotic_worlds$stampDimensionTable(table);
                return decoded;
            });
        }

        private static <T> @Nullable MapCodec<CarriedShape> stampCodecOf(MapLike<T> map) {
            if (map.get(SHAPE_KEY) != null) {
                return CARRIED_CODEC;
            }

            return map.get(FORMER_SHAPE_KEY) != null ? FORMER_CARRIED_CODEC : null;
        }
    }

    private StampedGeneratorCodec() {
    }
}
