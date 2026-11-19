package com.exoticworlds.client.shape.custom;

import java.util.function.Consumer;

import com.ascendiacore.client.gui.bridge.ComponentTree;
import com.ascendiacore.gui.layout.Column;
import com.ascendiacore.gui.layout.Row;
import com.ascendiacore.gui.layout.model.Length;
import com.ascendiacore.gui.theme.Theme;
import com.ascendiacore.gui.theme.Themes;
import com.ascendiacore.gui.widget.button.Button;
import com.ascendiacore.gui.widget.heading.Heading;
import com.ascendiacore.gui.widget.panel.model.FillKind;
import com.exoticworlds.client.shape.ShapeLines;
import com.exoticworlds.shape.WorldLoopPresets;
import com.exoticworlds.shape.custom.CustomSettings;
import com.uber.nullaway.annotations.Initializer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class WorldShapeSettingsScreen extends Screen {
    private static final Component TITLE = Component.translatable("gui.exotic_worlds.world_shape_settings");
    private static final Theme THEME = Themes.VANILLA;
    private static final int CONTENT_WIDTH = 468;
    private static final int SIDE_MARGIN = 10;
    private static final int PRESET_GAP = 5;
    private static final int FOOTER_GAP = 8;
    private static final int FOOTER_BUTTON_WIDTH = 150;

    private final Screen parent;
    private final Consumer<CustomSettings> onDone;
    private final CustomSettings chosen;
    private HeaderAndFooterLayout layout;
    private ComponentTree contents;

    public WorldShapeSettingsScreen(Screen parent, CustomSettings current, Consumer<CustomSettings> onDone) {
        super(TITLE);
        this.parent = parent;
        this.onDone = onDone;
        this.chosen = current;
    }

    @Override
    @Initializer
    protected void init() {
        this.layout = new HeaderAndFooterLayout(this);
        this.layout.addToHeader(new ComponentTree(header()));
        this.contents = this.layout.addToContents(new ComponentTree(body()));
        this.layout.addToFooter(new ComponentTree(this.footer()));
        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        this.contents.setSize(Math.min(CONTENT_WIDTH, this.width - 2 * SIDE_MARGIN), this.layout.getContentHeight());
        this.layout.arrangeElements();
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(this.parent);
    }

    private static Column header() {
        Column header = new Column();
        header.add(Heading.of(THEME.panel().fills().of(FillKind.PARCHMENT), TITLE));
        return header;
    }

    private static Column body() {
        Column body = new Column();
        body.add(presetRow());
        return body;
    }

    private static Row presetRow() {
        Row row = new Row();
        row.setGap(PRESET_GAP);
        for (WorldLoopPresets preset : WorldLoopPresets.values()) {
            row.add(Button.text(THEME.button(), ShapeLines.presetLabel(preset), () -> {
            }), Length.fill());
        }

        return row;
    }

    private Row footer() {
        Row footer = new Row();
        footer.setGap(FOOTER_GAP);
        footer.add(Button.text(THEME.button(), CommonComponents.GUI_DONE, this::commit), Length.fixed(FOOTER_BUTTON_WIDTH));
        footer.add(Button.text(THEME.button(), CommonComponents.GUI_CANCEL, this::onClose),
                Length.fixed(FOOTER_BUTTON_WIDTH));
        return footer;
    }

    private void commit() {
        this.onDone.accept(this.chosen);
        this.onClose();
    }
}
