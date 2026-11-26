package com.exoticworlds.api.v2.shape;

import com.exoticworlds.api.v2.geometry.LoopSpansV2;
import org.jspecify.annotations.Nullable;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

/** One dimension a shape is chosen for. {@link ShapeV2#spansIn} works the dimension's spans out from it. */
public interface ShapeDimensionV2 extends ShapeValuesV2 {
    /** The dimension the shape is chosen for. */
    ResourceKey<LevelStem> getDimension();

    /**
     * The spans the Overworld came to, or {@code null} for the Overworld itself and under an Overworld that loops
     * nothing.
     */
    @Nullable LoopSpansV2 getOverworld();

    /** The coordinate scale of the Overworld's dimension type. */
    double getOverworldScale();

    /** The coordinate scale of this dimension's type, 8 in the Nether and 1 in the Overworld and the End. */
    double getCoordinateScale();
}
