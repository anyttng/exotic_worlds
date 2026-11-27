package com.exoticworlds.core.world;

import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.option.ShapeOption;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.StringRepresentable;

public record SpawnPoint(Mode mode, int chunkX, int chunkZ) {
    public static final String KEY = "spawn";
    private static final String X_KEY = "x";
    private static final String Z_KEY = "z";
    private static final int POSITION = 3;
    private static final String DEFAULT_NOT_STORED = "The default spawn point is the option's default and is never stored";

    public static final SpawnPoint DEFAULT = new SpawnPoint(Mode.DEFAULT, 0, 0);

    private static final Codec<SpawnPoint> POINT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf(X_KEY).forGetter(SpawnPoint::chunkX),
            Codec.INT.fieldOf(Z_KEY).forGetter(SpawnPoint::chunkZ)
    ).apply(instance, SpawnPoint::new));

    public static final Codec<SpawnPoint> CODEC = POINT_CODEC.validate(spawn -> spawn.isCustom()
            ? DataResult.success(spawn)
            : DataResult.error(() -> DEFAULT_NOT_STORED));

    public static final ShapeOption<SpawnPoint> OPTION = new ShapeOption<>(KEY, POSITION, CODEC, DEFAULT);

    public SpawnPoint {
        if (mode == Mode.DEFAULT) {
            chunkX = 0;
            chunkZ = 0;
        }
    }

    public SpawnPoint(int chunkX, int chunkZ) {
        this(Mode.CUSTOM, chunkX, chunkZ);
    }

    public static SpawnPoint of(WorldFold fold) {
        return fold.getOptions().get(OPTION);
    }

    public boolean isCustom() {
        return this.mode == Mode.CUSTOM;
    }

    public int getBlockX() {
        return this.chunkX * CoordinateConstants.CHUNK_WIDTH_BLOCKS;
    }

    public int getBlockZ() {
        return this.chunkZ * CoordinateConstants.CHUNK_WIDTH_BLOCKS;
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
