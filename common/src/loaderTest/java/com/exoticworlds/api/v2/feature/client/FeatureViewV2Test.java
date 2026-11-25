package com.exoticworlds.api.v2.feature.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.ascendiacore.gui.base.component.GuiComponent;
import com.ascendiacore.gui.theme.Theme;
import com.ascendiacore.gui.widget.panel.model.FillStyle;
import com.exoticworlds.api.v2.feature.FeaturesV2;
import com.exoticworlds.api.v2.feature.OptionFeatureV2;
import com.exoticworlds.api.v2.geometry.LoopSpansV2;
import com.exoticworlds.bridge.v2.feature.FeatureBridge;
import com.exoticworlds.bridge.v2.feature.client.FeatureViewBridge;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.world.WorldLoopBounds;
import com.exoticworlds.shape.feature.client.FeaturePanel;
import com.exoticworlds.shape.feature.client.FeatureViews;
import com.exoticworlds.shape.feature.client.LineView;
import com.mojang.serialization.Codec;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

class FeatureViewV2Test {
    private static final String NAMESPACE = "example_mod";

    private static final int WIDTH_CHUNKS = 32;

    private static final int CHOSEN = 7;

    private static final FlatShape SPANS = ShapeFixtures.torus(WorldLoopBounds.ofWidth(WIDTH_CHUNKS));

    private static final Counted LINED = new Counted("feature_view_v2_test_lined");

    private static final Counted VIEWED = new Counted("feature_view_v2_test_viewed");

    private static final LineViewV2<Integer> LINE =
            (value, spans) -> Component.literal(value + " of " + spans.getWidthChunks(Direction.Axis.X));

    @BeforeAll
    static void registerFeatures() {
        FeaturesV2.register(LINED);
        FeaturesV2.register(VIEWED);
        FeatureViewsV2.registerLine(LINED, LINE);
    }

    @Test
    void aLineIsWorkedOutFromTheValueAndTheSpans() {
        assertEquals(CHOSEN + " of " + WIDTH_CHUNKS,
                FeatureViewBridge.internal(LINE).line().apply(CHOSEN, SPANS).getString());
    }

    @Test
    void aRegisteredLineIsTheFeaturesView() {
        assertTrue(FeatureViews.of(FeatureBridge.optionOf(LINED)) instanceof LineView<Integer>);
    }

    @Test
    void aFeatureHasOneView() {
        assertThrows(IllegalStateException.class, () -> FeatureViewsV2.registerLine(LINED, LINE));
        assertThrows(IllegalStateException.class, () -> FeatureViewsV2.register(LINED, panel -> {
        }));
    }

    @Test
    void aViewOfAnUnregisteredFeatureIsRefused() {
        Counted unregistered = new Counted("feature_view_v2_test_unregistered");

        assertThrows(IllegalStateException.class, () -> FeatureViewsV2.registerLine(unregistered, LINE));
    }

    @Test
    void aViewReadsThePageAndHandsItsValueBackThroughThePanel() {
        FakePanel page = new FakePanel(CHOSEN, SPANS);
        List<FeaturePanelV2<Integer>> seen = new ArrayList<>();
        FeatureViewV2<Integer> view = panel -> {
            seen.add(panel);
            panel.onShow(shown -> page.shownToView = shown);
            panel.takeValueFrom(() -> CHOSEN + 1);
        };

        FeatureViewBridge.internal(view).extend(page);
        FeaturePanelV2<Integer> panel = seen.get(0);
        page.shown.accept(CHOSEN);

        assertEquals(CHOSEN, panel.getValue());
        assertEquals(LoopSpansV2.ofWidth(WIDTH_CHUNKS), panel.getSpans());
        assertTrue(panel.isOffered());
        assertNull(panel.getCreationScreen());
        assertEquals(CHOSEN, page.shownToView);
        assertEquals(CHOSEN + 1, page.chosen.get());
    }

    @Test
    void aPageThatLoopsNothingGivesTheViewNoSpans() {
        FakePanel page = new FakePanel(CHOSEN, null);
        List<FeaturePanelV2<Integer>> seen = new ArrayList<>();

        FeatureViewBridge.internal((FeatureViewV2<Integer>) seen::add).extend(page);

        assertNull(seen.get(0).getSpans());
        assertSame(page.getValue(), seen.get(0).getValue());
    }

    private static final class Counted extends OptionFeatureV2<Integer> {
        Counted(String path) {
            super(Identifier.fromNamespaceAndPath(NAMESPACE, path), Codec.INT, 0, 0,
                    "example_mod.feature_view_v2_test.counted", List.of());
        }
    }

    private static final class FakePanel implements FeaturePanel<Integer> {
        private final Integer value;
        private final @Nullable FlatShape spans;
        private Consumer<Integer> shown = value -> {
        };
        private Supplier<@Nullable Integer> chosen = () -> null;
        private @Nullable Integer shownToView;

        FakePanel(Integer value, @Nullable FlatShape spans) {
            this.value = value;
            this.spans = spans;
        }

        @Override
        public Integer getValue() {
            return this.value;
        }

        @Override
        public @Nullable FlatShape getSpans() {
            return this.spans;
        }

        @Override
        public @Nullable Screen getCreationScreen() {
            return null;
        }

        @Override
        public boolean isOffered() {
            return true;
        }

        @Override
        public Theme getTheme() {
            throw new UnsupportedOperationException();
        }

        @Override
        public FillStyle getFill() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(GuiComponent component) {
        }

        @Override
        public void onShow(Consumer<Integer> shown) {
            this.shown = shown;
        }

        @Override
        public void takeValueFrom(Supplier<@Nullable Integer> chosen) {
            this.chosen = chosen;
        }
    }
}
