package com.exoticworlds.api.v2.shape;

import com.exoticworlds.api.v2.geometry.LoopSpansV2;
import org.jspecify.annotations.Nullable;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

/**
 * One dimension that follows the Overworld: the player chose no shape for it, so the Overworld's shape decides its
 * spans in {@link ShapeV2#derivedSpans}.
 */
public interface DerivedDimensionV2 {
    /** The dimension that follows the Overworld. */
    ResourceKey<LevelStem> getDimension();

    /** The spans of the Overworld. */
    LoopSpansV2 getOverworld();

    /** The coordinate scale of the Overworld's dimension type. */
    double getOverworldScale();

    /** The coordinate scale of this dimension's type: 8 in the Nether, 1 in the Overworld and the End. */
    double getCoordinateScale();

    /**
     * The Overworld's spans scaled by the two coordinate scales, so one lap of this dimension is one lap of the
     * Overworld: the rule a shape follows unless it overrides {@link ShapeV2#derivedSpans}. {@code null} where the
     * scaled widths are not whole chunks or fall outside the sizes a looped axis allows.
     */
    @Nullable LoopSpansV2 scaledCopy();
}
