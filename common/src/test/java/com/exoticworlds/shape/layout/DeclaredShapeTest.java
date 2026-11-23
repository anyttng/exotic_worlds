package com.exoticworlds.shape.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v2.option.DimensionOption;
import com.exoticworlds.api.v2.option.OptionPart;
import com.exoticworlds.api.v2.option.ShapeOption;
import com.exoticworlds.api.v2.option.ShapeOptions;
import com.exoticworlds.api.v2.shape.DimensionContext;
import com.exoticworlds.api.v2.shape.DimensionShape;
import com.exoticworlds.api.v2.shape.LoopSpans;
import com.exoticworlds.core.DimensionTable;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.OptionKey;
import com.exoticworlds.core.OptionKeys;
import com.exoticworlds.core.OptionValues;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.shape.ShapeOptionsAdapter;
import com.exoticworlds.shape.WorldLoopPresets;
import com.mojang.serialization.Codec;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

class DeclaredShapeTest {
    private static final double NETHER_SCALE = 8.0;

    private static final ShapeOption<Integer> SHAPE_WIDTH =
            new ShapeOption<>("example_band_width", 200, Codec.INT, 64);

    private static final OptionKey<Integer> WIDTH = ShapeOptionsAdapter.optionKey(SHAPE_WIDTH);

    private static final OptionValues PLAIN = DimensionLayout.DEFAULT_OPTIONS;

    private static final OptionValues WIDE = PLAIN.with(WIDTH, 128);

    private static final DimensionShape BAND = new Band();

    private static final DimensionShape STRIP = new Strip();

    @BeforeAll
    static void registerShapes() {
        if (!OptionKeys.all().contains(WIDTH)) {
            SHAPE_WIDTH.register();
        }

        if (DeclaredShapes.of(BAND.id()) == null) {
            BAND.register();
            STRIP.register();
        }
    }

    @Test
    void aShapeWorksItsSpansOutFromTheOptionsOnTheOverworldAndFromTheOverworldElsewhere() {
        FlatShape overworld = DeclaredShapes.resolve(BAND, context(LevelStem.OVERWORLD, WIDE, null));

        assertEquals(ShapeFixtures.cylinder(Direction.Axis.X, 128), overworld);
        assertEquals(ShapeFixtures.cylinder(Direction.Axis.X, 16),
                DeclaredShapes.resolve(BAND, context(LevelStem.NETHER, PLAIN, overworld)));
        assertEquals(ShapeFixtures.cylinder(Direction.Axis.X, 128),
                DeclaredShapes.resolve(BAND, context(LevelStem.END, PLAIN, overworld)));
    }

    @Test
    void aShapeThatLoopsNoAxisLeavesTheDimensionAsTheGameMakesIt() {
        FlatShape none = DeclaredShapes.resolve(BAND, context(LevelStem.OVERWORLD, PLAIN.with(WIDTH, 0), null));
        DimensionLayout layout = DimensionLayout.declared(BAND.id(), none, PLAIN);

        assertNull(none);
        assertEquals(ShapeKind.NORMAL, layout.kind());
        assertNull(layout.spans());
        assertEquals(BAND.id(), layout.shape());
    }

    @Test
    void aStoredDimensionShowsTheShapeWhereItsSpansComeBackEqual() {
        WorldLayout stored = new WorldLayout(
                DimensionLayout.looped(ShapeFixtures.cylinder(Direction.Axis.X, 128), WIDE),
                DimensionLayout.looped(ShapeFixtures.cylinder(Direction.Axis.X, 16), PLAIN),
                DimensionLayout.looped(ShapeFixtures.torus(96), PLAIN), Map.of(), 8);

        WorldLayout recognized = ShapeRecognition.recognize(stored, DeclaredShapeTest::scale);

        assertEquals(BAND.id(), recognized.overworld().shape());
        assertEquals(BAND.id(), recognized.nether().shape());
        assertNull(recognized.end().shape());
        assertEquals(stored.end(), recognized.end());
    }

    @Test
    void aDimensionOfACreatedWorldShowsTheShapeAndIsSentAsItsSpans() {
        ResourceKey<LevelStem> sky = ResourceKey.create(Registries.LEVEL_STEM,
                Identifier.fromNamespaceAndPath("example_mod", "sky"));
        FlatShape spans = ShapeFixtures.cylinder(Direction.Axis.X, 128);
        DimensionTable.Entry stored = LayoutDimensions.entryOf(DimensionLayout.looped(spans, WIDE));

        WorldLayout recognized = ShapeRecognition.recognize(
                LayoutDimensions.ofRows(Map.of(sky, stored)), DeclaredShapeTest::scale);

        assertEquals(DimensionLayout.declared(BAND.id(), spans, WIDE), recognized.modDimension(sky));
        assertEquals(stored, LayoutDimensions.entryOf(recognized.modDimension(sky)));
    }

    @Test
    void aPresetOnAShapeThatTakesOneChangesItsOptionsAndTheSpansTheyComeTo() {
        WorldLayout band = WorldLayout.DEFAULT.withOverworld(
                DimensionLayout.declared(BAND.id(), ShapeFixtures.cylinder(Direction.Axis.X, 128), WIDE));

        WorldLayout large = LayoutPreset.apply(band, WorldLoopPresets.LARGE, 1.0);

        assertTrue(LayoutPreset.offered(band));
        assertEquals(DimensionLayout.declared(BAND.id(), ShapeFixtures.cylinder(Direction.Axis.X, 256),
                PLAIN.with(WIDTH, 256)), large.overworld());
        assertEquals(WorldLoopPresets.LARGE.netherScale(), large.netherScale());
        assertTrue(LayoutPreset.matches(large, WorldLoopPresets.LARGE));
        assertFalse(LayoutPreset.matches(large, WorldLoopPresets.SMALL));
    }

    @Test
    void aShapeThatTakesNoPresetLeavesTheLayoutAsItIs() {
        WorldLayout strip = WorldLayout.DEFAULT.withOverworld(
                DimensionLayout.declared(STRIP.id(), ShapeFixtures.cylinder(Direction.Axis.Z, 48), PLAIN));

        assertFalse(LayoutPreset.offered(strip));
        assertSame(strip, LayoutPreset.apply(strip, WorldLoopPresets.LARGE, 1.0));
        assertFalse(LayoutPreset.matches(strip, WorldLoopPresets.LARGE));
        assertTrue(LayoutPreset.offered(WorldLayout.DEFAULT));
    }

    @Test
    void anOptionNamingAShapeNobodyDeclaredIsReported() {
        Identifier ghost = Identifier.fromNamespaceAndPath("example_mod", "ghost");
        DimensionOption<Integer> narrowed = DimensionOption.of(SHAPE_WIDTH, "gui.example_mod.band_width",
                List.of(OptionPart.number(0, 512))).forShapes(List.of(BAND.id(), ghost));

        assertEquals(List.of("option=example_band_width shape=example_mod:ghost"),
                DeclaredShapes.unknownOptionShapes(List.of(narrowed), Set.of(BAND.id())));
    }

    private static DimensionContext context(ResourceKey<LevelStem> dimension, OptionValues options,
            FlatShape overworld) {
        return DeclaredShapes.context(dimension, ShapeOptionsAdapter.shapeOptions(options), overworld, 1.0,
                scale(dimension));
    }

    private static double scale(ResourceKey<LevelStem> dimension) {
        return dimension == LevelStem.NETHER ? NETHER_SCALE : 1.0;
    }

    private static final class Band implements DimensionShape {
        @Override
        public Identifier id() {
            return Identifier.fromNamespaceAndPath("example_mod", "band");
        }

        @Override
        public LoopSpans spans(DimensionContext context) {
            LoopSpans overworld = context.overworld();
            int chunkWidth = overworld == null
                    ? context.options().get(SHAPE_WIDTH)
                    : (int) (overworld.chunkWidth(Direction.Axis.X) * context.overworldScale()
                            / context.coordinateScale());
            return chunkWidth == 0 ? LoopSpans.NONE : LoopSpans.ofWidth(Direction.Axis.X, chunkWidth);
        }

        @Override
        public boolean takesPresets() {
            return true;
        }

        @Override
        public ShapeOptions withPreset(int chunkWidth, ShapeOptions options) {
            return options.with(SHAPE_WIDTH, chunkWidth);
        }
    }

    private static final class Strip implements DimensionShape {
        @Override
        public Identifier id() {
            return Identifier.fromNamespaceAndPath("example_mod", "strip");
        }

        @Override
        public LoopSpans spans(DimensionContext context) {
            return LoopSpans.ofWidth(Direction.Axis.Z, 48);
        }
    }
}
