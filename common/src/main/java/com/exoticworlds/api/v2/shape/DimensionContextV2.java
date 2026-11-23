package com.exoticworlds.api.v2.shape;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v2.option.ShapeOptionsV2;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

/**
 * What a {@link DimensionShapeV2} works one dimension's spans out from.
 *
 * @param dimension the dimension the shape is chosen for
 * @param options the options chosen on that dimension's page
 * @param overworld the spans the Overworld came to, or {@code null} for the Overworld itself and under an Overworld
 *        that loops nothing
 * @param overworldScale the coordinate scale of the Overworld's dimension type
 * @param coordinateScale the coordinate scale of this dimension's type
 */
public record DimensionContextV2(ResourceKey<LevelStem> dimension, ShapeOptionsV2 options,
        @Nullable LoopSpansV2 overworld, double overworldScale, double coordinateScale) {
}
