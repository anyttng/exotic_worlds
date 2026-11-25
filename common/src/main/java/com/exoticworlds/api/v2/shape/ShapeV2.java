package com.exoticworlds.api.v2.shape;

import com.exoticworlds.api.v2.geometry.LoopSpansV2;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v2.ExperimentalV2;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * A world shape another mod adds. Extend it, implement {@link #spansIn}, and register one instance with
 * {@link ShapesV2#register} while your mod is constructed. The shape is then offered on every dimension's page of the
 * World Shape Settings screen.
 *
 * <p>What the player tunes on the page are the shape's parameters ({@link #getParameters()}) and, under them, the
 * features attached to it ({@link ShapesV2#attach}). A world stores the shape's id with each dimension, so a world
 * being re-created opens on the shape it was made with.</p>
 */
public abstract class ShapeV2 {
    private final Identifier id;
    private final String nameKey;
    private final String hintKey;

    /**
     * @param id the id the shape is saved under, in your mod's namespace
     * @param nameKey the translation key of the shape's name
     * @param hintKey the translation key of its one-line description
     */
    protected ShapeV2(Identifier id, String nameKey, String hintKey) {
        this.id = id;
        this.nameKey = nameKey;
        this.hintKey = hintKey;
    }

    /** The id the shape is saved under. */
    public final Identifier getId() {
        return this.id;
    }

    /** The translation key of the shape's name. */
    public final String getNameKey() {
        return this.nameKey;
    }

    /** The translation key of the shape's one-line description. */
    public final String getHintKey() {
        return this.hintKey;
    }

    /**
     * The spans of one dimension the shape is chosen for. Spans that loop no axis leave the dimension as the game
     * makes it; a looped axis outside the sizes the dimension allows holds the screen's Done.
     */
    public abstract LoopSpansV2 spansIn(ShapeDimensionV2 dimension);

    /** The values the shape is tuned with, in the order the page shows them. None by default. */
    public List<ParameterV2<?>> getParameters() {
        return List.of();
    }

    /** The one-line summary of a dimension on this shape, shown in the dimension list. The name by default. */
    public Component describe(ShapeValuesV2 values) {
        return Component.translatable(this.nameKey);
    }

    /**
     * The spans of a dimension that follows an Overworld on this shape, or {@code null} to leave that dimension as
     * the game makes it. By default {@link DerivedDimensionV2#scaledCopy()}.
     */
    @ExperimentalV2
    public @Nullable LoopSpansV2 derivedSpans(DerivedDimensionV2 dimension) {
        return dimension.scaledCopy();
    }
}
