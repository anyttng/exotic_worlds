package com.exoticworlds.api.v2.feature.client;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.ascendiacore.gui.base.component.GuiComponent;
import com.ascendiacore.gui.theme.Theme;
import com.ascendiacore.gui.widget.panel.model.FillStyle;
import com.exoticworlds.api.v2.ExperimentalV2;
import com.exoticworlds.api.v2.geometry.LoopSpansV2;

import net.minecraft.client.gui.screens.Screen;

/**
 * The place on one dimension's page a {@link FeatureViewV2} puts its widgets, under the feature's own controls, and
 * what the page tells it.
 *
 * @param <T> the type of the feature's value
 */
@ExperimentalV2
public interface FeaturePanelV2<T> {
    /** The value chosen on the page right now, or {@code null} while what the player typed is not a value. */
    @Nullable T getValue();

    /** The spans the dimension comes to with the current choices, or {@code null} while it loops nothing. */
    @Nullable LoopSpansV2 getSpans();

    /** The create-world screen the settings were opened from, or {@code null} when they were opened elsewhere. */
    @Nullable Screen getCreationScreen();

    /** Whether the shape chosen on the page carries the feature. Hide your widgets while it does not. */
    boolean isOffered();

    /** The theme of the screen, for widgets that take one. */
    Theme getTheme();

    /** The fill of the page, for widgets that draw over it. */
    FillStyle getFill();

    /** Adds a widget under the feature's controls. Widgets stack top to bottom in the order they are added. */
    void add(GuiComponent component);

    /** Calls {@code shown} with the stored value each time the page is shown, so that you set your widgets from it. */
    void onShow(Consumer<T> shown);

    /**
     * Makes your widgets the source of the value. {@code chosen} is asked when the player confirms, and a {@code null}
     * answer holds the screen's Done.
     */
    void takeValueFrom(Supplier<@Nullable T> chosen);
}
