package com.exoticworlds.shape.cylinder;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Direction;

public record CylinderSize(Direction.Axis axis, int widthChunks) {
    private static final String AXIS_KEY = "axis";
    private static final String WIDTH_KEY = "width_chunks";

    static final Codec<CylinderSize> CODEC = RecordCodecBuilder.create(size -> size.group(
            Direction.Axis.CODEC.fieldOf(AXIS_KEY).forGetter(CylinderSize::axis),
            Codec.INT.fieldOf(WIDTH_KEY).forGetter(CylinderSize::widthChunks)).apply(size, CylinderSize::new));
}
