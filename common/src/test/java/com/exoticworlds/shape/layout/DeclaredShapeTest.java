package com.exoticworlds.shape.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.exoticworlds.core.world.StemTerrain;
import com.exoticworlds.bridge.v2.geometry.ShapeSpansAdapter;
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
import com.exoticworlds.shape.option.DimensionOption;
import com.mojang.serialization.Codec;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

class DeclaredShapeTest {
    private static final double NETHER_SCALE = 8.0;

    private static final ShapeOption<Integer> WIDTH = new ShapeOption<>("example_band_width", 200, Codec.INT, 64);

    private static final ShapeOptions PLAIN = LayoutFixtures.PLAIN;

    private static final ShapeOptions WIDE = PLAIN.with(WIDTH, 128);

    private static final DimensionShape BAND = new Band();

    private static final DimensionShape STRIP = new Strip();

    @BeforeAll
    static void registerShapes() {
        if (!ShapeOptionRegistry.getAll().contains(WIDTH)) {
            ShapeOptionRegistry.register(WIDTH);
        }

        if (DeclaredShapes.of(BAND.getId()) == null) {
            DeclaredShapes.register(BAND);
            DeclaredShapes.register(STRIP);
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
        assertEquals(DimensionMode.SHAPED, layout.mode());
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
        WorldLayout band = WorldLayout.newWorld().withOverworld(
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
        WorldLayout strip = WorldLayout.newWorld().withOverworld(
                DimensionLayout.shaped(STRIP.getId(), ShapeFixtures.cylinder(Direction.Axis.Z, 48), PLAIN));

        assertFalse(LayoutPreset.isOffered(strip));
        assertSame(strip, LayoutPreset.apply(strip, WorldLoopPresets.LARGE, 1.0));
        assertFalse(LayoutPreset.isMatching(strip, WorldLoopPresets.LARGE));
        assertTrue(LayoutPreset.isOffered(WorldLayout.newWorld()));
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

    private abstract static class FixtureShape implements DimensionShape {
        private final Identifier id;

        FixtureShape(String path) {
            this.id = Identifier.fromNamespaceAndPath("example_mod", path);
        }

        @Override
        public final Identifier getId() {
            return this.id;
        }

        @Override
        public final Component getName() {
            return Component.literal(this.id.getPath());
        }

        @Override
        public final Component getHint() {
            return getName();
        }

        @Override
        public final Component describe(ShapeOptions options) {
            return getName();
        }

        @Override
        public final List<DimensionOption<?>> getParameters() {
            return List.of();
        }

        @Override
        public final ShapeOptions defaultOptions(StemTerrain stem) {
            return ShapeOptions.DEFAULT;
        }

        @Override
        public boolean canTakePresets() {
            return false;
        }

        @Override
        public ShapeOptions withPreset(WorldLoopPresets preset, ShapeOptions options) {
            return options;
        }

        @Override
        public final Optional<ShapeOptions> optionsFor(FlatShape spans) {
            return Optional.empty();
        }
    }

    private static final class Band extends FixtureShape {
        Band() {
            super("band");
        }

        @Override
        public FlatShape spans(DimensionContext context) {
            FlatShape overworld = context.overworld();
            int widthChunks = overworld == null
                    ? context.options().get(WIDTH)
                    : (int) (ShapeSpansAdapter.loopSpans(overworld).getWidthChunks(Direction.Axis.X) * context.overworldScale()
                            / context.coordinateScale());
            return widthChunks == 0 ? FlatShape.rectangle() : ShapeFixtures.cylinder(Direction.Axis.X, widthChunks);
        }

        @Override
        public boolean canTakePresets() {
            return true;
        }

        @Override
        public ShapeOptions withPreset(WorldLoopPresets preset, ShapeOptions options) {
            return options.with(WIDTH, preset.getWidthChunks());
        }
    }

    private static final class Strip extends FixtureShape {
        Strip() {
            super("strip");
        }

        @Override
        public FlatShape spans(DimensionContext context) {
            return ShapeFixtures.cylinder(Direction.Axis.Z, 48);
        }
    }
}
