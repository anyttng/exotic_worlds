package com.exoticworlds.api.v2.shape;

import com.exoticworlds.bridge.v2.shape.ShapeBridge;

import net.minecraft.resources.Identifier;

/** The ids of the shapes Exotic Worlds itself offers for a dimension, for a declaration that names a shape. */
public final class BuiltInShapesV2 {
    /** Both horizontal axes loop. */
    public static final Identifier TORUS = ShapeBridge.getTorus();

    /** One horizontal axis loops, the other runs on. */
    public static final Identifier CYLINDER = ShapeBridge.getCylinder();

    private BuiltInShapesV2() {
    }
}
