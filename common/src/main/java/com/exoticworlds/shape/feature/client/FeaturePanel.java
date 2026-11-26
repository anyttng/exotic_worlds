package com.exoticworlds.shape.feature.client;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.ascendiacore.gui.base.component.GuiComponent;
import com.ascendiacore.gui.theme.Theme;
import com.ascendiacore.gui.widget.panel.model.FillStyle;
import com.exoticworlds.core.world.FlatShape;

import net.minecraft.client.gui.screens.Screen;

public interface FeaturePanel<T> {
    @Nullable T getValue();

    @Nullable FlatShape getSpans();

    @Nullable Screen getCreationScreen();

    boolean isOffered();

    Theme getTheme();

    FillStyle getFill();

    void addComponent(GuiComponent component);

    void onShow(Consumer<T> shown);

    void takeValueFrom(Supplier<@Nullable T> chosen);

    void setDisabledNumberText(String partKey, Function<T, String> text);
}
