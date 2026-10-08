package com.exoticworlds.api.v1.shape;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.core.CarriedShape;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.engine.gen.ShapedDimensions;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;

/**
 * The one door a shape has into the world's dimensions: it writes the declared spans into the generators of the three
 * vanilla stems, and reads them back out of an existing world. Everything a folding world then does — the wrapped
 * noise, the packet rewriting, the seam behaviour — is derived from what is written here and is not reachable from
 * this package.
 *
 * <p>Call {@link #withSpans} from {@link ShapeModule.Apply} and {@link #spansOf} from {@link ShapeModule.Read}. The
 * dimensions handed to {@code Apply} have had any earlier shape stripped, so a shape states its whole geometry rather
 * than amending someone else's.</p>
 */
public final class ShapeDimensions {

    /**
     * The dimensions with each of the three vanilla stems carrying its declared spans, skew included, and the world's
     * options. A stem whose generator cannot take a shape is left alone, so a datapack dimension of another mod is
     * never rewritten.
     */
    public static WorldDimensions withSpans(WorldDimensions dimensions, LoopSpans overworld, LoopSpans nether,
            LoopSpans end, GenerationOptions options) {
        return ShapedDimensions.withShapes(dimensions,
                carried(overworld, options),
                carried(nether, options),
                carried(end, options));
    }

    /**
     * The spans that stem was created with, skew included, or {@code null} where it carries no shape of ours or one
     * spans cannot state. A shape reads this in {@link ShapeModule.Read} and refuses anything that is not its own
     * geometry — normally by checking which axes {@link LoopSpans#loops} answers for and its
     * {@link LoopSpans#skewChunks()}.
     */
    public static @Nullable LoopSpans spansOf(WorldDimensions dimensions, ResourceKey<LevelStem> key) {
        FlatShape shape = ShapedDimensions.shapeOf(dimensions, key);
        return shape == null || shape.mirror() != null ? null : new LoopSpans(shape.bounds(), shape.skewChunks());
    }

    /**
     * The world options that stem was created with, or {@link GenerationOptions#DEFAULT} where it carries no shape of
     * ours. Options are stored per stem but written once for the whole world, so the overworld's are the world's.
     */
    public static GenerationOptions optionsOf(WorldDimensions dimensions, ResourceKey<LevelStem> key) {
        CarriedShape carried = ShapedDimensions.carriedShapeOf(dimensions, key);
        return carried == null ? GenerationOptions.DEFAULT : carried.generationOptions();
    }

    private static CarriedShape carried(LoopSpans spans, GenerationOptions options) {
        return new CarriedShape(FlatShape.of(spans), options);
    }

    private ShapeDimensions() {
    }
}
