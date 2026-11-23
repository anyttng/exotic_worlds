package com.exoticworlds.shape.layout;

import java.util.Map;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.NetherScales;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.ShapeOptions;
import com.exoticworlds.core.WorldLoopSizes;

import net.minecraft.core.Direction;

public final class LayoutFixtures {
    public static final ShapeOptions PLAIN = DimensionLayout.DEFAULT_OPTIONS;

    public static WorldLayout shaped(FlatShape overworld, int netherScale, FlatShape end, ShapeOptions options) {
        return new WorldLayout(DimensionLayout.looped(overworld, options), DimensionLayout.fromOverworld(options),
                DimensionLayout.looped(end, options), Map.of(), netherScale);
    }

    public static WorldLayout defaultTorus() {
        return shaped(ShapeFixtures.torus(WorldLoopSizes.DEFAULT_CHUNK_WIDTH), NetherScales.DEFAULT,
                ShapeFixtures.torus(WorldLoopSizes.END_DEFAULT_CHUNK_WIDTH), PLAIN);
    }

    public static WorldLayout defaultCylinder() {
        return shaped(ShapeFixtures.cylinder(Direction.Axis.X, WorldLoopSizes.DEFAULT_CHUNK_WIDTH), NetherScales.DEFAULT,
                ShapeFixtures.cylinder(Direction.Axis.X, WorldLoopSizes.END_DEFAULT_CHUNK_WIDTH), PLAIN);
    }

    private LayoutFixtures() {
    }
}
