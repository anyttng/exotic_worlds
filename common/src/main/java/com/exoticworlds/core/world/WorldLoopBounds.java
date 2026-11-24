package com.exoticworlds.core.world;

import java.util.Optional;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.Direction;
import net.minecraft.network.VarInt;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.border.WorldBorder;

public record WorldLoopBounds(AxisBounds x, AxisBounds z) {

    public sealed interface AxisBounds {
        String MIN_CHUNK_KEY = "min_chunk";
        String MAX_CHUNK_KEY = "max_chunk";

        String UNBOUNDED_TEXT = "unbounded";

        record Looped(int minChunk, int maxChunk) implements AxisBounds {
            private static final String ASCENDING_SPAN_REQUIRED = "A looped axis needs minChunk < maxChunk, got [";

            public Looped {
                if (minChunk >= maxChunk) {
                    throw new IllegalArgumentException(ASCENDING_SPAN_REQUIRED + minChunk + ", " + maxChunk + ")");
                }
            }

            public static Looped ofWidth(int widthChunks) {
                int minChunk = -(widthChunks / 2);
                return new Looped(minChunk, widthChunks + minChunk);
            }

            public int getWidthChunks() {
                return maxChunk - minChunk;
            }

            public int getMinBlock() {
                return minChunk * CoordinateConstants.CHUNK_WIDTH_BLOCKS;
            }

            public int getMaxBlock() {
                return maxChunk * CoordinateConstants.CHUNK_WIDTH_BLOCKS;
            }

            @Override
            public int getMaxViewDistanceChunks() {
                return Math.max(1, getWidthChunks() / 2 - CoordinateConstants.VIEW_DISTANCE_MARGIN_CHUNKS);
            }

            @Override
            public String getSpanText() {
                return "[" + minChunk + ".." + maxChunk + ")";
            }
        }

        record Unbounded() implements AxisBounds {
            public static final Unbounded INSTANCE = new Unbounded();

            @Override
            public int getMaxViewDistanceChunks() {
                return Integer.MAX_VALUE;
            }

            @Override
            public String getSpanText() {
                return UNBOUNDED_TEXT;
            }
        }

        int getMaxViewDistanceChunks();

        String getSpanText();

        StreamCodec<ByteBuf, AxisBounds> STREAM_CODEC = StreamCodec.of(
                (buffer, axis) -> {
                    switch (axis) {
                        case Looped looped -> {
                            buffer.writeBoolean(true);
                            VarInt.write(buffer, looped.minChunk());
                            VarInt.write(buffer, looped.maxChunk());
                        }
                        case Unbounded() -> buffer.writeBoolean(false);
                    }
                },
                buffer -> buffer.readBoolean()
                        ? new Looped(VarInt.read(buffer), VarInt.read(buffer))
                        : Unbounded.INSTANCE);

        Codec<AxisBounds> CODEC = Codec.mapPair(
                        Codec.INT.optionalFieldOf(MIN_CHUNK_KEY),
                        Codec.INT.optionalFieldOf(MAX_CHUNK_KEY))
                .codec()
                .flatXmap(AxisBounds::read, AxisBounds::write);

        private static DataResult<AxisBounds> read(Pair<Optional<Integer>, Optional<Integer>> bounds) {
            Optional<Integer> minChunk = bounds.getFirst();
            Optional<Integer> maxChunk = bounds.getSecond();
            if (minChunk.isEmpty() && maxChunk.isEmpty()) {
                return DataResult.success(Unbounded.INSTANCE);
            }

            if (minChunk.isEmpty() || maxChunk.isEmpty()) {
                return DataResult.error(() -> "An axis needs both chunk bounds or neither, got only one of "
                        + MIN_CHUNK_KEY + "/" + MAX_CHUNK_KEY);
            }

            if (minChunk.get() == -LEGACY_DISABLED_AXIS_RADIUS_CHUNKS
                    || maxChunk.get() == LEGACY_DISABLED_AXIS_RADIUS_CHUNKS) {
                return DataResult.success(Unbounded.INSTANCE);
            }

            try {
                return DataResult.success(new Looped(minChunk.get(), maxChunk.get()));
            } catch (IllegalArgumentException refused) {
                return DataResult.error(refused::getMessage);
            }
        }

        private static DataResult<Pair<Optional<Integer>, Optional<Integer>>> write(AxisBounds axis) {
            return DataResult.success(switch (axis) {
                case Looped looped -> Pair.of(Optional.of(looped.minChunk()), Optional.of(looped.maxChunk()));
                case Unbounded() -> Pair.of(Optional.empty(), Optional.empty());
            });
        }
    }

    private static final int LEGACY_DISABLED_AXIS_RADIUS_CHUNKS =
            (int) (WorldBorder.MAX_CENTER_COORDINATE / CoordinateConstants.CHUNK_WIDTH_BLOCKS) + 8192;

    private static final String X_KEY = "x";
    private static final String Z_KEY = "z";

    private static final String NOT_A_HORIZONTAL_AXIS = "Not a horizontal axis: ";

    public static final MapCodec<WorldLoopBounds> MAP_CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    AxisBounds.CODEC.fieldOf(X_KEY).forGetter(WorldLoopBounds::x),
                    AxisBounds.CODEC.fieldOf(Z_KEY).forGetter(WorldLoopBounds::z)
            ).apply(instance, instance.stable(WorldLoopBounds::new)));

    public static final Codec<WorldLoopBounds> CODEC = MAP_CODEC.codec();

    public static final StreamCodec<ByteBuf, WorldLoopBounds> STREAM_CODEC = StreamCodec.composite(
            AxisBounds.STREAM_CODEC, WorldLoopBounds::x,
            AxisBounds.STREAM_CODEC, WorldLoopBounds::z,
            WorldLoopBounds::new);

    public static final WorldLoopBounds UNBOUNDED =
            new WorldLoopBounds(AxisBounds.Unbounded.INSTANCE, AxisBounds.Unbounded.INSTANCE);

    public static WorldLoopBounds ofWidth(int widthChunks) {
        AxisBounds.Looped looped = AxisBounds.Looped.ofWidth(widthChunks);
        return new WorldLoopBounds(looped, looped);
    }

    public static WorldLoopBounds ofWidths(int xWidthChunks, int zWidthChunks) {
        return new WorldLoopBounds(AxisBounds.Looped.ofWidth(xWidthChunks), AxisBounds.Looped.ofWidth(zWidthChunks));
    }

    public static WorldLoopBounds ofWidth(Direction.Axis axis, int widthChunks) {
        return UNBOUNDED.with(axis, AxisBounds.Looped.ofWidth(widthChunks));
    }

    public WorldLoopBounds with(Direction.Axis axis, AxisBounds bounds) {
        return switch (axis) {
            case X -> new WorldLoopBounds(bounds, z);
            case Z -> new WorldLoopBounds(x, bounds);
            case Y -> throw new IllegalArgumentException(NOT_A_HORIZONTAL_AXIS + axis);
        };
    }

    public AxisBounds getAxis(Direction.Axis axis) {
        return switch (axis) {
            case X -> x;
            case Z -> z;
            case Y -> throw new IllegalArgumentException(NOT_A_HORIZONTAL_AXIS + axis);
        };
    }

    public boolean isLooped(Direction.Axis axis) {
        return getAxis(axis) instanceof AxisBounds.Looped;
    }

    public int getWidthChunks(Direction.Axis axis) {
        return switch (getAxis(axis)) {
            case AxisBounds.Looped looped -> looped.getWidthChunks();
            case AxisBounds.Unbounded() -> throw new IllegalStateException("chunkWidth() of the unbounded axis " + axis);
        };
    }

    public WorldLoopBounds scaledDown(int scale) {
        return new WorldLoopBounds(scaledDown(x, scale), scaledDown(z, scale));
    }

    private static AxisBounds scaledDown(AxisBounds axis, int scale) {
        return switch (axis) {
            case AxisBounds.Looped looped -> AxisBounds.Looped.ofWidth(looped.getWidthChunks() / scale);
            case AxisBounds.Unbounded() -> axis;
        };
    }

    public boolean isSquare() {
        return x instanceof AxisBounds.Looped isXLooped
                && z instanceof AxisBounds.Looped isZLooped
                && isXLooped.getWidthChunks() == isZLooped.getWidthChunks();
    }

    public int getMaxViewDistanceChunks() {
        return Math.min(x.getMaxViewDistanceChunks(), z.getMaxViewDistanceChunks());
    }
}
