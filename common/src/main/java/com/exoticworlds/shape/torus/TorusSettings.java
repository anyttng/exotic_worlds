package com.exoticworlds.shape.torus;

import java.util.List;

import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.api.v1.option.WorldOption;
import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.NetherScales;
import com.exoticworlds.core.WorldLoopSizes;
import com.exoticworlds.shape.climate.ClimateScale;
import com.exoticworlds.shape.climate.CompactBiomes;

import net.minecraft.core.Direction;

public record TorusSettings(LoopSpans overworld, int netherScale, LoopSpans end,
        GenerationOptions generationOptions) {
    public static final List<WorldOption<?>> OFFERED_OPTIONS =
            List.of(CompactBiomes.OPTION, GuaranteedLand.OPTION, GuaranteedNetherComplexes.OPTION);

    private static final GenerationOptions DEFAULT_GENERATION_OPTIONS =
            GenerationOptions.DEFAULT.with(CompactBiomes.OPTION, ClimateScale.OFF);

    public static final TorusSettings DEFAULT = new TorusSettings(
            LoopSpans.ofWidth(WorldLoopSizes.DEFAULT_CHUNK_WIDTH),
            NetherScales.DEFAULT,
            LoopSpans.ofWidth(WorldLoopSizes.END_DEFAULT_CHUNK_WIDTH),
            DEFAULT_GENERATION_OPTIONS);

    public int chunkWidth(Direction.Axis axis) {
        return overworld.chunkWidth(axis);
    }

    public int skewChunks() {
        return overworld.skewChunks();
    }

    public int endChunkWidth() {
        return end.chunkWidth(Direction.Axis.X);
    }

    public static boolean isTorus(LoopSpans spans) {
        return spans.loops(Direction.Axis.X) && spans.loops(Direction.Axis.Z);
    }

    public static boolean isPlainSquare(LoopSpans spans) {
        return spans.isSquare() && spans.skewChunks() == FlatShape.NO_SKEW;
    }
}
