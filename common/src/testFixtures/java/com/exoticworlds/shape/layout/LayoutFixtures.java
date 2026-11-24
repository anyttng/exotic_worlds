package com.exoticworlds.shape.layout;

import java.util.Map;

import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.NetherScales;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.WorldLoopSizes;

import net.minecraft.core.Direction;

public final class LayoutFixtures {
    public static final ShapeOptions PLAIN = DimensionLayout.DEFAULT_OPTIONS;

    public static WorldLayout shaped(FlatShape overworld, int netherScale, FlatShape end, ShapeOptions options) {
        return new WorldLayout(DimensionLayout.looped(overworld, options), DimensionLayout.fromOverworld(options),
                DimensionLayout.looped(end, options), Map.of(), netherScale);
    }

    public static WorldLayout defaultTorus() {
        return shaped(ShapeFixtures.torus(WorldLoopSizes.DEFAULT_WIDTH_CHUNKS), NetherScales.DEFAULT,
                ShapeFixtures.torus(WorldLoopSizes.END_DEFAULT_WIDTH_CHUNKS), PLAIN);
    }

    public static WorldLayout defaultCylinder() {
        return shaped(ShapeFixtures.cylinder(Direction.Axis.X, WorldLoopSizes.DEFAULT_WIDTH_CHUNKS), NetherScales.DEFAULT,
                ShapeFixtures.cylinder(Direction.Axis.X, WorldLoopSizes.END_DEFAULT_WIDTH_CHUNKS), PLAIN);
    }

    private LayoutFixtures() {
    }
}
