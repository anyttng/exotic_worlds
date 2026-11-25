package com.exoticworlds.shape.dimension;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.minecraft.resources.Identifier;

class DeclaredShapesTest {
    private static final Identifier BAND = Identifier.fromNamespaceAndPath("example_mod", "band");

    @Test
    void aShapeFeaturesAreAttachedToButNobodyDeclaredIsReported() {
        Identifier ghost = Identifier.fromNamespaceAndPath("example_mod", "ghost");

        assertEquals(List.of(ghost), DeclaredShapes.unknownShapes(List.of(BAND, ghost), Set.of(BAND)));
    }
}
