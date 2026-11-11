package com.exoticworlds.core;

import com.exoticworlds.api.v1.option.WorldOption;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.StringRepresentable;

public record SpawnPoint(Mode mode, int xChunk, int zChunk) {
    public static final String KEY = "spawn";
    private static final String X_KEY = "x";
    private static final String Z_KEY = "z";
    private static final int POSITION = 3;
    private static final String DEFAULT_NOT_STORED = "The default spawn point is the option's default and is never stored";

    public static final SpawnPoint DEFAULT = new SpawnPoint(Mode.DEFAULT, 0, 0);

    private static final Codec<SpawnPoint> POINT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf(X_KEY).forGetter(SpawnPoint::xChunk),
            Codec.INT.fieldOf(Z_KEY).forGetter(SpawnPoint::zChunk)
    ).apply(instance, SpawnPoint::new));

    public static final Codec<SpawnPoint> CODEC = POINT_CODEC.validate(spawn -> spawn.isCustom()
            ? DataResult.success(spawn)
            : DataResult.error(() -> DEFAULT_NOT_STORED));

    public static final WorldOption<SpawnPoint> OPTION = new WorldOption<>(KEY, POSITION, CODEC, DEFAULT);

    public SpawnPoint {
        if (mode == Mode.DEFAULT) {
            xChunk = 0;
            zChunk = 0;
        }
    }

    public SpawnPoint(int xChunk, int zChunk) {
        this(Mode.CUSTOM, xChunk, zChunk);
    }

    public static SpawnPoint of(WorldFold fold) {
        return fold.generationOptions().get(OPTION);
    }

    public boolean isCustom() {
        return this.mode == Mode.CUSTOM;
    }

    public int blockX() {
        return this.xChunk * CoordinateConstants.CHUNK_WIDTH;
    }

    public int blockZ() {
        return this.zChunk * CoordinateConstants.CHUNK_WIDTH;
    }

    public enum Mode implements StringRepresentable {
        DEFAULT("default"),
        CUSTOM("custom");

        private final String serializedName;

        Mode(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return this.serializedName;
        }
    }
}
