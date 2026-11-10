package com.exoticworlds.core;

import com.exoticworlds.api.v1.option.WorldOption;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record SpawnPoint(int xChunk, int zChunk) {
    public static final String KEY = "spawn";
    private static final String X_KEY = "x";
    private static final String Z_KEY = "z";
    private static final int POSITION = 3;

    public static final SpawnPoint ZERO = new SpawnPoint(0, 0);

    public static final Codec<SpawnPoint> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf(X_KEY).forGetter(SpawnPoint::xChunk),
            Codec.INT.fieldOf(Z_KEY).forGetter(SpawnPoint::zChunk)
    ).apply(instance, SpawnPoint::new));

    public static final WorldOption<SpawnPoint> OPTION = new WorldOption<>(KEY, POSITION, CODEC, ZERO);

    public static SpawnPoint of(WorldFold fold) {
        return fold.generationOptions().get(OPTION);
    }

    public int blockX() {
        return this.xChunk * CoordinateConstants.CHUNK_WIDTH;
    }

    public int blockZ() {
        return this.zChunk * CoordinateConstants.CHUNK_WIDTH;
    }
}
