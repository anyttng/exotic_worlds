package com.exoticworlds.api.v2.shape;

import com.exoticworlds.ExoticWorlds;

import net.minecraft.resources.Identifier;

/** The ids of the shapes Exotic Worlds itself offers for a dimension, for a declaration that names a shape. */
public final class BuiltInShapes {
    private static final String TORUS_PATH = "torus";
    private static final String CYLINDER_PATH = "cylinder";

    /** Both horizontal axes loop. */
    public static final Identifier TORUS = Identifier.fromNamespaceAndPath(ExoticWorlds.MODID, TORUS_PATH);

    /** One horizontal axis loops, the other runs on. */
    public static final Identifier CYLINDER = Identifier.fromNamespaceAndPath(ExoticWorlds.MODID, CYLINDER_PATH);

    private BuiltInShapes() {
    }
}
