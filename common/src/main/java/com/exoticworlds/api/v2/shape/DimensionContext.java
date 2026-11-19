package com.exoticworlds.api.v2.shape;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.api.v1.shape.LoopSpans;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

/**
 * What a {@link DimensionShape} works one dimension's spans out from.
 *
 * @param dimension the dimension the shape is chosen for
 * @param options the options chosen on that dimension's page
 * @param overworld the spans the Overworld came to, or {@code null} for the Overworld itself and under an Overworld
 *        that loops nothing
 * @param overworldScale the coordinate scale of the Overworld's dimension type
 * @param coordinateScale the coordinate scale of this dimension's type
 */
public record DimensionContext(ResourceKey<LevelStem> dimension, GenerationOptions options,
        @Nullable LoopSpans overworld, double overworldScale, double coordinateScale) {
}
