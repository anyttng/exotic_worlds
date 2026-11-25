package com.exoticworlds.shape.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v2.option.ShapeOptionV2;
import com.exoticworlds.api.v2.option.ShapeOptionsV2;
import com.exoticworlds.api.v2.shape.DimensionContextV2;
import com.exoticworlds.api.v2.shape.DimensionShapeV2;
import com.exoticworlds.api.v2.shape.LoopSpansV2;
import com.exoticworlds.bridge.v2.option.ShapeOptionsAdapter;
import com.exoticworlds.core.world.DimensionTable;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.option.ShapeOption;
import com.exoticworlds.core.option.ShapeOptionRegistry;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.shape.WorldLoopPresets;
import com.exoticworlds.shape.dimension.DeclaredShapes;
import com.exoticworlds.shape.dimension.DimensionContext;
import com.exoticworlds.shape.dimension.DimensionShape;
import com.mojang.serialization.Codec;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

class DeclaredShapeTest {
    private static final double NETHER_SCALE = 8.0;

    private static final ShapeOptionV2<Integer> SHAPE_WIDTH =
            new ShapeOptionV2<>("example_band_width", 200, Codec.INT, 64);

    private static final ShapeOption<Integer> WIDTH = ShapeOptionsAdapter.fromV2(SHAPE_WIDTH);

    private static final ShapeOptions PLAIN = DimensionLayout.DEFAULT_OPTIONS;

    private static final ShapeOptions WIDE = PLAIN.with(WIDTH, 128);

    private static final DimensionShapeV2 BAND = new Band();

    private static final DimensionShapeV2 STRIP = new Strip();

    @BeforeAll
    static void registerShapes() {
        if (!ShapeOptionRegistry.getAll().contains(WIDTH)) {
            SHAPE_WIDTH.register();
        }

        if (DeclaredShapes.of(BAND.getId()) == null) {
            BAND.register();
            STRIP.register();
        }
    }

    @Test
    void aShapeWorksItsSpansOutFromTheOptionsOnTheOverworldAndFromTheOverworldElsewhere() {
        FlatShape overworld = DeclaredShapes.resolve(band(), context(LevelStem.OVERWORLD, WIDE, null));

        assertEquals(ShapeFixtures.cylinder(Direction.Axis.X, 128), overworld);
        assertEquals(ShapeFixtures.cylinder(Direction.Axis.X, 16),
                DeclaredShapes.resolve(band(), context(LevelStem.NETHER, PLAIN, overworld)));
        assertEquals(ShapeFixtures.cylinder(Direction.Axis.X, 128),
                DeclaredShapes.resolve(band(), context(LevelStem.END, PLAIN, overworld)));
    }

    @Test
    void aShapeThatLoopsNoAxisLeavesTheDimensionAsTheGameMakesIt() {
        FlatShape none = DeclaredShapes.resolve(band(), context(LevelStem.OVERWORLD, PLAIN.with(WIDTH, 0), null));
        DimensionLayout layout = DimensionLayout.shaped(BAND.getId(), none, PLAIN);

        assertNull(none);
        assertEquals(ShapeKind.SHAPED, layout.kind());
        assertNull(layout.spans());
        assertEquals(BAND.getId(), layout.shape());
    }

    @Test
    void aDimensionStoredWithItsShapeReadsBackAsThatShape() {
        ResourceKey<LevelStem> sky = sky();
        FlatShape spans = ShapeFixtures.cylinder(Direction.Axis.X, 128);
        DimensionTable.Entry stored = LayoutDimensions.entryOf(DimensionLayout.shaped(BAND.getId(), spans, WIDE));

        assertEquals(DimensionLayout.shaped(BAND.getId(), spans, WIDE),
                LayoutDimensions.ofRows(Map.of(sky, stored)).getModDimension(sky));
    }

    private static ResourceKey<LevelStem> sky() {
        return ResourceKey.create(Registries.LEVEL_STEM, Identifier.fromNamespaceAndPath("example_mod", "sky"));
    }

    @Test
    void aPresetOnAShapeThatTakesOneChangesItsOptionsAndTheSpansTheyComeTo() {
        WorldLayout band = WorldLayout.DEFAULT.withOverworld(
                DimensionLayout.shaped(BAND.getId(), ShapeFixtures.cylinder(Direction.Axis.X, 128), WIDE));

        WorldLayout large = LayoutPreset.apply(band, WorldLoopPresets.LARGE, 1.0);

        assertTrue(LayoutPreset.isOffered(band));
        assertEquals(DimensionLayout.shaped(BAND.getId(), ShapeFixtures.cylinder(Direction.Axis.X, 256),
                PLAIN.with(WIDTH, 256)), large.overworld());
        assertEquals(WorldLoopPresets.LARGE.getNetherScale(), large.netherScale());
        assertTrue(LayoutPreset.isMatching(large, WorldLoopPresets.LARGE));
        assertFalse(LayoutPreset.isMatching(large, WorldLoopPresets.SMALL));
    }

    @Test
    void aShapeThatTakesNoPresetLeavesTheLayoutAsItIs() {
        WorldLayout strip = WorldLayout.DEFAULT.withOverworld(
                DimensionLayout.shaped(STRIP.getId(), ShapeFixtures.cylinder(Direction.Axis.Z, 48), PLAIN));

        assertFalse(LayoutPreset.isOffered(strip));
        assertSame(strip, LayoutPreset.apply(strip, WorldLoopPresets.LARGE, 1.0));
        assertFalse(LayoutPreset.isMatching(strip, WorldLoopPresets.LARGE));
        assertTrue(LayoutPreset.isOffered(WorldLayout.DEFAULT));
    }

    private static DimensionContext context(ResourceKey<LevelStem> dimension, ShapeOptions options,
            @Nullable FlatShape overworld) {
        return new DimensionContext(dimension, options, overworld, 1.0, scale(dimension));
    }

    private static DimensionShape band() {
        return Objects.requireNonNull(DeclaredShapes.of(BAND.getId()));
    }

    private static double scale(ResourceKey<LevelStem> dimension) {
        return dimension == LevelStem.NETHER ? NETHER_SCALE : 1.0;
    }

    private static final class Band implements DimensionShapeV2 {
        @Override
        public Identifier getId() {
            return Identifier.fromNamespaceAndPath("example_mod", "band");
        }

        @Override
        public LoopSpansV2 spans(DimensionContextV2 context) {
            LoopSpansV2 overworld = context.overworld();
            int widthChunks = overworld == null
                    ? context.options().get(SHAPE_WIDTH)
                    : (int) (overworld.getWidthChunks(Direction.Axis.X) * context.overworldScale()
                            / context.coordinateScale());
            return widthChunks == 0 ? LoopSpansV2.NONE : LoopSpansV2.ofWidth(Direction.Axis.X, widthChunks);
        }

        @Override
        public boolean canTakePresets() {
            return true;
        }

        @Override
        public ShapeOptionsV2 withPreset(int widthChunks, ShapeOptionsV2 options) {
            return options.with(SHAPE_WIDTH, widthChunks);
        }
    }

    private static final class Strip implements DimensionShapeV2 {
        @Override
        public Identifier getId() {
            return Identifier.fromNamespaceAndPath("example_mod", "strip");
        }

        @Override
        public LoopSpansV2 spans(DimensionContextV2 context) {
            return LoopSpansV2.ofWidth(Direction.Axis.Z, 48);
        }
    }
}
