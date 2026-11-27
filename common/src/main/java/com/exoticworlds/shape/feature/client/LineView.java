package com.exoticworlds.shape.feature.client;

import java.util.function.BiFunction;

import com.ascendiacore.gui.widget.label.Label;
import com.ascendiacore.gui.widget.label.model.LabelRole;
import com.exoticworlds.core.world.FlatShape;

import net.minecraft.network.chat.Component;

public record LineView<T>(BiFunction<T, FlatShape, Component> line) implements FeatureView<T> {
    @Override
    public void extendPanel(FeaturePanel<T> panel) {
        Label label = new Label(panel.getFill(), LabelRole.MUTED, Component.empty());
        label.setText(() -> this.text(panel));
        label.setVisible(() -> panel.isOffered() && !this.text(panel).getString().isEmpty());
        panel.addComponent(label);
    }

    private Component text(FeaturePanel<T> panel) {
        T value = panel.getValue();
        FlatShape spans = panel.getSpans();
        return value == null || spans == null ? Component.empty() : this.line.apply(value, spans);
    }
}
