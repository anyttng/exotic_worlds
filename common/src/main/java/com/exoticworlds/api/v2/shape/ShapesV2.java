package com.exoticworlds.api.v2.shape;

import com.exoticworlds.api.v2.feature.FeatureV2;
import com.exoticworlds.bridge.v2.shape.ShapeBridge;

import net.minecraft.resources.Identifier;

/**
 * The world shapes: the ids of the built-in ones, the call that adds your own and the call that puts a feature on a
 * shape.
 *
 * <p>A feature does nothing until it is attached: register it with {@code FeaturesV2.register}, then attach it to each
 * shape it works on. The lines of a shape's page follow the order of these calls, after the shape's own parameters.</p>
 */
public final class ShapesV2 {
    /** The torus: both horizontal axes loop. */
    public static final Identifier TORUS = ShapeBridge.getTorus();

    /** The cylinder: one horizontal axis loops. */
    public static final Identifier CYLINDER = ShapeBridge.getCylinder();

    /**
     * Adds a shape to the World Shape Settings screen. Call it while your mod is constructed.
     *
     * @throws IllegalStateException if a shape is already registered under this id, if one of its parameters shares
     *         an id with a value already saved in worlds, or if a world has already started
     */
    public static void register(ShapeV2 shape) {
        ShapeBridge.register(shape);
    }

    /**
     * Attaches a registered feature to the shape with the id {@code shape}, in the dimensions of {@code scope}. The
     * shape may be a built-in one, your own or another mod's, and need not be registered yet: a shape nobody registers
     * by the time a world starts is reported in the log as {@code [ew/feature] unknown_shape}.
     *
     * @throws IllegalStateException if the feature was never registered, if the shape already carries it, or if a
     *         world has already started
     */
    public static void attach(Identifier shape, FeatureV2 feature, DimensionScopeV2 scope) {
        ShapeBridge.attach(shape, feature, scope);
    }

    private ShapesV2() {
    }
}
