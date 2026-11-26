package com.exoticworlds.api.v2.shape;

import com.exoticworlds.core.world.StemTerrain;
import com.exoticworlds.api.v2.geometry.LoopSpansV2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v2.feature.OptionPartV2;
import com.exoticworlds.core.option.ShapeOption;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.DerivedDimension;
import com.exoticworlds.core.world.ShapeDerivations;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.shape.WorldLoopPresets;
import com.exoticworlds.shape.dimension.DeclaredShapes;
import com.exoticworlds.shape.dimension.DimensionContext;
import com.exoticworlds.shape.dimension.DimensionShape;
import com.mojang.serialization.Codec;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

class ShapeV2Test {
    private static final String NAMESPACE = "example_mod";

    private static final String BAND_NAME_KEY = "gui.example_mod.shape_v2_test.band";

    private static final double NETHER_SCALE = 8.0;

    private static final int STORED_WIDTH_CHUNKS = 64;

    private static final int NETHER_WIDTH_CHUNKS = 16;

    private static final int CHOSEN_WIDTH_CHUNKS = 128;

    private static final int MIN_WIDTH_CHUNKS = 0;

    private static final int MAX_WIDTH_CHUNKS = 1024;

    private static final int STRIP_WIDTH_CHUNKS = 48;

    private static final ParameterV2<Integer> WIDTH = ParameterV2
            .of(id("shape_v2_test_band_width"), Codec.INT, STORED_WIDTH_CHUNKS, "gui.example_mod.shape_v2_test.width",
                    List.of(OptionPartV2.number(MIN_WIDTH_CHUNKS, MAX_WIDTH_CHUNKS)))
            .withNewWorldValue(
                    dimension -> dimension == LevelStem.NETHER ? NETHER_WIDTH_CHUNKS : STORED_WIDTH_CHUNKS)
            .withPresetWidth((widthChunks, presetWidthChunks) -> presetWidthChunks);

    private static final ParameterV2<Integer> NOBODYS = ParameterV2.of(id("shape_v2_test_nobodys"), Codec.INT, 0,
            "gui.example_mod.shape_v2_test.nobodys", List.of());

    private static final ShapeV2 BAND = new Band();

    private static final ShapeV2 STRIP = new Strip();

    @BeforeAll
    static void registerShapes() {
        ShapesV2.register(BAND);
        ShapesV2.register(STRIP);
    }

    @Test
    void aShapeWorksItsSpansOutFromItsParameterOnTheOverworldAndFromTheOverworldElsewhere() {
        FlatShape overworld = DeclaredShapes.resolve(band(), context(LevelStem.OVERWORLD, chosen(), null));

        assertEquals(ShapeFixtures.cylinder(Direction.Axis.X, CHOSEN_WIDTH_CHUNKS), overworld);
        assertEquals(ShapeFixtures.cylinder(Direction.Axis.X, CHOSEN_WIDTH_CHUNKS / (int) NETHER_SCALE),
                DeclaredShapes.resolve(band(), context(LevelStem.NETHER, ShapeOptions.DEFAULT, overworld)));
    }

    @Test
    void aDimensionThatNeverStatedTheParameterReadsItsStoredDefault() {
        assertEquals(ShapeFixtures.cylinder(Direction.Axis.X, STORED_WIDTH_CHUNKS),
                DeclaredShapes.resolve(band(), context(LevelStem.OVERWORLD, ShapeOptions.DEFAULT, null)));
    }

    @Test
    void aNewWorldStartsEachDimensionAtTheParametersValueForIt() {
        assertEquals(STORED_WIDTH_CHUNKS, band().defaultOptions(StemTerrain.ofKey(LevelStem.OVERWORLD)).get(widthOption()));
        assertEquals(NETHER_WIDTH_CHUNKS, band().defaultOptions(StemTerrain.ofKey(LevelStem.NETHER)).get(widthOption()));
    }

    @Test
    void theParameterIsShownOnTheShapesPageUnderItsCaption() {
        assertEquals(List.of(WIDTH.getCaptionKey()),
                band().getParameters().stream().map(parameter -> parameter.getCaptionKey()).toList());
        assertEquals(List.of(), strip().getParameters());
    }

    @Test
    void aPresetRewritesTheParameterThatTakesIt() {
        assertTrue(band().canTakePresets());
        assertEquals(WorldLoopPresets.LARGE.getWidthChunks(),
                band().withPreset(WorldLoopPresets.LARGE, chosen()).get(widthOption()));
    }

    @Test
    void aShapeWithoutAPresetParameterTakesNoPreset() {
        assertFalse(strip().canTakePresets());
        assertEquals(ShapeOptions.DEFAULT, strip().withPreset(WorldLoopPresets.LARGE, ShapeOptions.DEFAULT));
    }

    @Test
    void aDimensionThatFollowsTheOverworldTakesTheScaledCopyByDefault() {
        FlatShape overworld = ShapeFixtures.cylinder(Direction.Axis.X, CHOSEN_WIDTH_CHUNKS);

        assertEquals(ShapeFixtures.cylinder(Direction.Axis.X, CHOSEN_WIDTH_CHUNKS / (int) NETHER_SCALE),
                ShapeDerivations.derive(BAND.getId(), overworld,
                        DerivedDimension.ofKey(LevelStem.NETHER, 1.0, NETHER_SCALE)));
    }

    @Test
    void aShapeDecidesWhatFollowsItsOverworld() {
        FlatShape overworld = ShapeFixtures.cylinder(Direction.Axis.Z, STRIP_WIDTH_CHUNKS);

        assertNull(ShapeDerivations.derive(STRIP.getId(), overworld,
                DerivedDimension.ofKey(LevelStem.NETHER, 1.0, NETHER_SCALE)));
        assertEquals(overworld, ShapeDerivations.derive(STRIP.getId(), overworld,
                DerivedDimension.ofKey(LevelStem.END, 1.0, 1.0)));
    }

    @Test
    void theSummaryIsTheShapesNameUnlessItSaysMore() {
        assertEquals(BAND_NAME_KEY, keyOf(band().getName()));
        assertEquals(BAND_NAME_KEY, keyOf(band().describe(chosen())));
        assertEquals(String.valueOf(STRIP_WIDTH_CHUNKS), strip().describe(ShapeOptions.DEFAULT).getString());
    }

    @Test
    void aSecondShapeUnderOneIdIsRefused() {
        assertThrows(IllegalStateException.class, () -> ShapesV2.register(new Strip()));
    }

    @Test
    void aParameterTheShapeNeverReturnedIsNamedWhenItsValueIsAsked() {
        ShapeV2 careless = new ShapeV2(id("shape_v2_test_careless"), BAND_NAME_KEY, BAND_NAME_KEY) {
            @Override
            public LoopSpansV2 spansIn(ShapeDimensionV2 dimension) {
                return LoopSpansV2.ofWidth(Direction.Axis.X, dimension.getValue(NOBODYS));
            }
        };
        ShapesV2.register(careless);
        DimensionShape registered = Objects.requireNonNull(DeclaredShapes.of(careless.getId()));

        IllegalStateException refused = assertThrows(IllegalStateException.class,
                () -> registered.spans(context(LevelStem.OVERWORLD, ShapeOptions.DEFAULT, null)));

        assertTrue(refused.getMessage().contains(NOBODYS.getId().toString()));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(NAMESPACE, path);
    }

    private static String keyOf(Component component) {
        return ((TranslatableContents) component.getContents()).getKey();
    }

    private static ShapeOptions chosen() {
        return ShapeOptions.DEFAULT.with(widthOption(), CHOSEN_WIDTH_CHUNKS);
    }

    @SuppressWarnings("unchecked")
    private static ShapeOption<Integer> widthOption() {
        return (ShapeOption<Integer>) band().getParameters().get(0).getOption();
    }

    private static DimensionShape band() {
        return Objects.requireNonNull(DeclaredShapes.of(BAND.getId()));
    }

    private static DimensionShape strip() {
        return Objects.requireNonNull(DeclaredShapes.of(STRIP.getId()));
    }

    private static DimensionContext context(ResourceKey<LevelStem> dimension, ShapeOptions options,
            @Nullable FlatShape overworld) {
        return new DimensionContext(dimension, options, overworld, 1.0,
                dimension == LevelStem.NETHER ? NETHER_SCALE : 1.0);
    }

    private static final class Band extends ShapeV2 {
        Band() {
            super(id("shape_v2_test_band"), BAND_NAME_KEY, BAND_NAME_KEY + ".hint");
        }

        @Override
        public List<ParameterV2<?>> getParameters() {
            return List.of(WIDTH);
        }

        @Override
        public LoopSpansV2 spansIn(ShapeDimensionV2 dimension) {
            LoopSpansV2 overworld = dimension.getOverworld();
            int widthChunks = overworld == null
                    ? dimension.getValue(WIDTH)
                    : (int) (overworld.getWidthChunks(Direction.Axis.X) * dimension.getOverworldScale()
                            / dimension.getCoordinateScale());
            return LoopSpansV2.ofWidth(Direction.Axis.X, widthChunks);
        }
    }

    private static final class Strip extends ShapeV2 {
        Strip() {
            super(id("shape_v2_test_strip"), "gui.example_mod.shape_v2_test.strip",
                    "gui.example_mod.shape_v2_test.strip.hint");
        }

        @Override
        public LoopSpansV2 spansIn(ShapeDimensionV2 dimension) {
            return LoopSpansV2.ofWidth(Direction.Axis.Z, STRIP_WIDTH_CHUNKS);
        }

        @Override
        public Component describe(ShapeValuesV2 values) {
            return Component.literal(String.valueOf(STRIP_WIDTH_CHUNKS));
        }

        @Override
        public @Nullable LoopSpansV2 derivedSpans(DerivedDimensionV2 dimension) {
            return dimension.getDimension() == LevelStem.END ? dimension.getOverworld() : null;
        }
    }
}
