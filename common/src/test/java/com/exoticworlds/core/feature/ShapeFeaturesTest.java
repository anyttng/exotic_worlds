package com.exoticworlds.core.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.CarriedShape;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.shape.StartupRegistrations;
import com.exoticworlds.shape.dimension.DeclaredShapes;
import com.exoticworlds.shape.feature.guaranteedland.GuaranteedLand;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

class ShapeFeaturesTest {
    private static final Identifier TEST_SHAPE = Identifier.fromNamespaceAndPath("example_mod", "feature_test_shape");

    private static final Identifier UNKNOWN_SHAPE = Identifier.fromNamespaceAndPath("example_mod", "unknown_shape");

    private static final GenerationFeature FIRST = new Named("shape_features_test_first");

    private static final GenerationFeature SECOND = new Named("shape_features_test_second");

    private static final GenerationFeature OVERWORLD_ONLY = new Named("shape_features_test_overworld_only");

    private static final GenerationFeature NEVER_REGISTERED = new Named("shape_features_test_never_registered");

    private static final ShapeOptions LAND = ShapeOptions.DEFAULT.with(GuaranteedLand.FEATURE.getOption(), true);

    @BeforeAll
    static void registerFeatures() {
        StartupRegistrations.options();
        ShapeFeatures.register(FIRST);
        ShapeFeatures.register(SECOND);
        ShapeFeatures.register(OVERWORLD_ONLY);
        ShapeFeatures.attach(TEST_SHAPE, SECOND, DimensionScope.ALL);
        ShapeFeatures.attach(TEST_SHAPE, FIRST, DimensionScope.ALL);
        ShapeFeatures.attach(TEST_SHAPE, OVERWORLD_ONLY, DimensionScope.of(List.of(LevelStem.OVERWORLD)));
    }

    @Test
    void featuresComeInTheGlobalOrderRatherThanTheOrderTheShapeTookThem() {
        assertEquals(List.of(FIRST, SECOND, OVERWORLD_ONLY),
                active(TEST_SHAPE, ShapeOptions.DEFAULT, LevelStem.OVERWORLD));
    }

    @Test
    void aScopeLeavesOutEveryDimensionItDoesNotName() {
        assertEquals(List.of(FIRST, SECOND), active(TEST_SHAPE, ShapeOptions.DEFAULT, LevelStem.NETHER));
    }

    @Test
    void aFeatureThatIsOffForTheOptionsIsLeftOut() {
        assertFalse(active(DeclaredShapes.TORUS, ShapeOptions.DEFAULT, LevelStem.OVERWORLD)
                .contains(GuaranteedLand.FEATURE));
        assertTrue(active(DeclaredShapes.TORUS, LAND, LevelStem.OVERWORLD).contains(GuaranteedLand.FEATURE));
    }

    @Test
    void aShapeNoFeatureWasAttachedToHasNone() {
        assertEquals(List.of(), active(UNKNOWN_SHAPE, LAND, LevelStem.OVERWORLD));
    }

    @Test
    void aWorldStoredWithoutAShapeIdTakesTheFeaturesOfTheShapeItsSpansMake() {
        CarriedShape stored = new CarriedShape(null, ShapeFixtures.torus(64), LAND);

        assertTrue(ShapeFeatures.activeFeatures(stored, LevelStem.OVERWORLD).contains(GuaranteedLand.FEATURE));
        assertFalse(ShapeFeatures.activeFeatures(stored, LevelStem.NETHER).contains(GuaranteedLand.FEATURE));
    }

    @Test
    void aFeatureAttachedBeforeItIsRegisteredIsRefused() {
        assertThrows(IllegalStateException.class,
                () -> ShapeFeatures.attach(TEST_SHAPE, NEVER_REGISTERED, DimensionScope.ALL));
    }

    @Test
    void aFeatureAttachedTwiceToOneShapeIsRefused() {
        assertThrows(IllegalStateException.class, () -> ShapeFeatures.attach(TEST_SHAPE, FIRST, DimensionScope.ALL));
    }

    private static List<GenerationFeature> active(Identifier shape, ShapeOptions options,
            ResourceKey<LevelStem> dimension) {
        return ShapeFeatures.activeFeatures(new CarriedShape(shape, ShapeFixtures.torus(64), options), dimension);
    }

    private static final class Named implements GenerationFeature {
        private final String key;

        private Named(String key) {
            this.key = key;
        }

        @Override
        public String getKey() {
            return this.key;
        }
    }
}
