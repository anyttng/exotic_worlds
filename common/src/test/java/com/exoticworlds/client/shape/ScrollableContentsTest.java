package com.exoticworlds.client.shape;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;

class ScrollableContentsTest {
    private static final int TARGET_WIDTH = 100;
    private static final int TARGET_HEIGHT = 20;
    private static final int TARGET_COUNT = 4;
    private static final int WINDOW_HEIGHT = 2 * TARGET_HEIGHT;
    private static final int WINDOW_X = 10;
    private static final int WINDOW_Y = 50;
    private static final int CLICK_X = WINDOW_X + TARGET_WIDTH / 2;
    private static final int LEFT_BUTTON = 0;
    private static final double ONE_TARGET_DOWN = -2.0;

    private final List<Target> targets = new ArrayList<>();
    private ScrollableContents contents;

    @BeforeEach
    void layOut() {
        LinearLayout column = LinearLayout.vertical();
        for (int i = 0; i < TARGET_COUNT; i++) {
            this.targets.add(column.addChild(new Target()));
        }

        this.contents = new ScrollableContents(column);
        this.contents.setMaxHeight(WINDOW_HEIGHT);
        this.contents.setX(WINDOW_X);
        this.contents.setY(WINDOW_Y);
    }

    @Test
    void aClickAboveTheWindowReachesNoChild() {
        this.contents.mouseScrolled(CLICK_X, WINDOW_Y, 0.0, ONE_TARGET_DOWN);

        assertFalse(this.contents.mouseClicked(CLICK_X, WINDOW_Y - 1, LEFT_BUTTON));
        assertEquals(0, this.targets.get(0).clicks);
    }

    @Test
    void aClickBelowTheWindowReachesNoChild() {
        assertFalse(this.contents.mouseClicked(CLICK_X, WINDOW_Y + WINDOW_HEIGHT, LEFT_BUTTON));
        assertEquals(0, this.targets.get(2).clicks);
    }

    @Test
    void aClickInsideTheWindowReachesTheChildUnderTheCursor() {
        this.contents.mouseScrolled(CLICK_X, WINDOW_Y, 0.0, ONE_TARGET_DOWN);

        assertTrue(this.contents.mouseClicked(CLICK_X, WINDOW_Y + TARGET_HEIGHT + 1, LEFT_BUTTON));
        assertEquals(1, this.targets.get(2).clicks);
        assertEquals(0, this.targets.get(1).clicks);
    }

    private static final class Target extends AbstractWidget {
        private int clicks;

        Target() {
            super(0, 0, TARGET_WIDTH, TARGET_HEIGHT, CommonComponents.EMPTY);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!this.clicked(mouseX, mouseY)) {
                return false;
            }

            this.clicks++;
            return true;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
        }
    }
}
