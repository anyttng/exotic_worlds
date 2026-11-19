package com.exoticworlds.shape.layout;

import java.util.Map;

import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.core.NetherScales;
import com.exoticworlds.core.WorldLoopSizes;

import net.minecraft.core.Direction;

public final class LayoutFixtures {
    public static final GenerationOptions PLAIN = DimensionLayout.DEFAULT_OPTIONS;

    public static WorldLayout shaped(LoopSpans overworld, int netherScale, LoopSpans end, GenerationOptions options) {
        return new WorldLayout(DimensionLayout.looped(overworld, options), DimensionLayout.fromOverworld(options),
                DimensionLayout.looped(end, options), Map.of(), netherScale);
    }

    public static WorldLayout defaultTorus() {
        return shaped(LoopSpans.ofWidth(WorldLoopSizes.DEFAULT_CHUNK_WIDTH), NetherScales.DEFAULT,
                LoopSpans.ofWidth(WorldLoopSizes.END_DEFAULT_CHUNK_WIDTH), PLAIN);
    }

    public static WorldLayout defaultCylinder() {
        return shaped(LoopSpans.ofWidth(Direction.Axis.X, WorldLoopSizes.DEFAULT_CHUNK_WIDTH), NetherScales.DEFAULT,
                LoopSpans.ofWidth(Direction.Axis.X, WorldLoopSizes.END_DEFAULT_CHUNK_WIDTH), PLAIN);
    }

    private LayoutFixtures() {
    }
}
