package com.exoticworlds.shape.dimension;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.option.ShapeOption;
import com.exoticworlds.shape.option.DeclaredOptions;
import com.exoticworlds.shape.option.DimensionOption;
import com.exoticworlds.shape.option.DimensionScope;
import com.exoticworlds.shape.option.OptionPart;
import com.mojang.serialization.Codec;

import net.minecraft.resources.Identifier;

class DeclaredShapesTest {
    private static final Identifier BAND = Identifier.fromNamespaceAndPath("example_mod", "band");

    private static final ShapeOption<Integer> WIDTH = new ShapeOption<>("example_band_width", 0, Codec.INT, 0);

    @Test
    void anOptionNamingAShapeNobodyDeclaredIsReported() {
        Identifier ghost = Identifier.fromNamespaceAndPath("example_mod", "ghost");
        DeclaredOptions.Entry narrowed = new DeclaredOptions.Entry(DimensionOption.of(WIDTH,
                "gui.example_mod.band_width", List.<OptionPart<Integer>>of()), List.of(BAND, ghost), DimensionScope.ALL);

        assertEquals(List.of("option=example_band_width shape=example_mod:ghost"),
                DeclaredShapes.unknownOptionShapes(List.of(narrowed), Set.of(BAND)));
    }
}
