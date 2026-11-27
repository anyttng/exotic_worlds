package com.exoticworlds.api.v2.feature.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v2.feature.FeaturesV2;
import com.exoticworlds.api.v2.feature.OptionFeatureV2;
import com.exoticworlds.bridge.v2.feature.FeatureBridge;
import com.exoticworlds.bridge.v2.feature.client.FeatureViewBridge;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.world.WorldLoopBounds;
import com.exoticworlds.shape.feature.client.FeatureViews;
import com.exoticworlds.shape.feature.client.LineView;
import com.mojang.serialization.Codec;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

class LineViewV2Test {
    private static final String NAMESPACE = "example_mod";

    private static final int WIDTH_CHUNKS = 32;

    private static final int CHOSEN = 7;

    private static final FlatShape SPANS = ShapeFixtures.torus(WorldLoopBounds.ofWidth(WIDTH_CHUNKS));

    private static final Counted LINED = new Counted("line_view_v2_test_lined");

    private static final LineViewV2<Integer> LINE =
            (value, spans) -> Component.literal(value + " of " + spans.getWidthChunks(Direction.Axis.X));

    @BeforeAll
    static void registerFeatures() {
        FeaturesV2.register(LINED);
        FeatureViewsV2.registerLine(LINED, LINE);
    }

    @Test
    void aLineIsWorkedOutFromTheValueAndTheSpans() {
        assertEquals(CHOSEN + " of " + WIDTH_CHUNKS,
                FeatureViewBridge.toInternal(LINE).line().apply(CHOSEN, SPANS).getString());
    }

    @Test
    void aRegisteredLineIsTheFeaturesView() {
        assertTrue(FeatureViews.of(FeatureBridge.optionOf(LINED)) instanceof LineView<Integer>);
    }

    @Test
    void aFeatureHasOneView() {
        assertThrows(IllegalStateException.class, () -> FeatureViewsV2.registerLine(LINED, LINE));
    }

    @Test
    void aViewOfAnUnregisteredFeatureIsRefused() {
        Counted unregistered = new Counted("line_view_v2_test_unregistered");

        assertThrows(IllegalStateException.class, () -> FeatureViewsV2.registerLine(unregistered, LINE));
    }

    private static final class Counted extends OptionFeatureV2<Integer> {
        Counted(String path) {
            super(Identifier.fromNamespaceAndPath(NAMESPACE, path), Codec.INT, 0, 0,
                    "example_mod.line_view_v2_test.counted", List.of());
        }
    }
}
