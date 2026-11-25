package com.exoticworlds.bridge.v2.feature.client;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.ascendiacore.gui.base.component.GuiComponent;
import com.ascendiacore.gui.theme.Theme;
import com.ascendiacore.gui.widget.panel.model.FillStyle;
import com.exoticworlds.api.v2.feature.client.FeaturePanelV2;
import com.exoticworlds.api.v2.geometry.LoopSpansV2;
import com.exoticworlds.bridge.v2.geometry.ShapeSpansAdapter;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.shape.feature.client.FeaturePanel;

import net.minecraft.client.gui.screens.Screen;

final class FeaturePanelView<T> implements FeaturePanelV2<T> {
    private final FeaturePanel<T> panel;

    FeaturePanelView(FeaturePanel<T> panel) {
        this.panel = panel;
    }

    @Override
    public @Nullable T getValue() {
        return this.panel.getValue();
    }

    @Override
    public @Nullable LoopSpansV2 getSpans() {
        FlatShape spans = this.panel.getSpans();
        return spans == null ? null : ShapeSpansAdapter.loopSpans(spans);
    }

    @Override
    public @Nullable Screen getCreationScreen() {
        return this.panel.getCreationScreen();
    }

    @Override
    public boolean isOffered() {
        return this.panel.isOffered();
    }

    @Override
    public Theme getTheme() {
        return this.panel.getTheme();
    }

    @Override
    public FillStyle getFill() {
        return this.panel.getFill();
    }

    @Override
    public void add(GuiComponent component) {
        this.panel.add(component);
    }

    @Override
    public void onShow(Consumer<T> shown) {
        this.panel.onShow(shown);
    }

    @Override
    public void takeValueFrom(Supplier<@Nullable T> chosen) {
        this.panel.takeValueFrom(chosen);
    }
}
