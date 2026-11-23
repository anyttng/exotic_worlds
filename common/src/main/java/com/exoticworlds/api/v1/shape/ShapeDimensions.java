package com.exoticworlds.api.v1.shape;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.bridge.v1.shape.ShapeBridge;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;

/**
 * The one door a shape has into the world's dimensions: it writes the declared spans into the generators of the three
 * vanilla stems, and reads them back out of an existing world. Everything a folding world then does — the wrapped
 * noise, the packet rewriting, the seam behaviour — is derived from what is written here and is not reachable from
 * this package.
 *
 * <p>A shape declared through {@code api.v2} writes nothing here itself: it answers its spans and Exotic Worlds
 * writes them. {@link #spansOf} and {@link #optionsOf} read a world's dimensions back.</p>
 */
public final class ShapeDimensions {

    /**
     * The dimensions with each of the three vanilla stems carrying its declared spans, skew included, and the world's
     * options. A stem whose generator cannot take a shape is left alone, so a datapack dimension of another mod is
     * never rewritten.
     *
     * @throws IllegalArgumentException if a looping axis of any of the three spans is narrower than
     *         {@link LoopSpans#minChunkWidth()}
     * @deprecated nothing calls this at world creation any more: it was what a {@link ShapeModule} applied, and no
     *     screen lists one. A {@link com.exoticworlds.api.v2.shape.DimensionShapeV2} answers its spans and Exotic Worlds
     *     writes them.
     */
    @Deprecated
    @SuppressWarnings("InlineMeSuggester")
    public static WorldDimensions withSpans(WorldDimensions dimensions, LoopSpans overworld, LoopSpans nether,
            LoopSpans end, GenerationOptions options) {
        return ShapeBridge.withSpans(dimensions, overworld, nether, end, options);
    }

    /**
     * The spans that stem was created with, skew included, or {@code null} where it carries no shape of ours or one
     * spans cannot state.
     */
    public static @Nullable LoopSpans spansOf(WorldDimensions dimensions, ResourceKey<LevelStem> key) {
        return ShapeBridge.spansOf(dimensions, key);
    }

    /**
     * The world options that stem was created with, or {@link GenerationOptions#DEFAULT} where it carries no shape of
     * ours. Options are stored per stem but written once for the whole world, so the overworld's are the world's.
     */
    public static GenerationOptions optionsOf(WorldDimensions dimensions, ResourceKey<LevelStem> key) {
        return ShapeBridge.optionsOf(dimensions, key);
    }

    private ShapeDimensions() {
    }
}
