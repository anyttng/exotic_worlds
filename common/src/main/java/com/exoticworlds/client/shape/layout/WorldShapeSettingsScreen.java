package com.exoticworlds.client.shape.layout;

import java.util.function.Consumer;

import com.ascendiacore.client.gui.bridge.ComponentTree;
import com.ascendiacore.gui.base.property.Property;
import com.ascendiacore.gui.layout.Column;
import com.ascendiacore.gui.layout.Row;
import com.ascendiacore.gui.layout.model.Length;
import com.ascendiacore.gui.theme.Theme;
import com.ascendiacore.gui.theme.Themes;
import com.ascendiacore.gui.widget.button.Button;
import com.ascendiacore.gui.widget.heading.Heading;
import com.ascendiacore.gui.widget.label.Label;
import com.ascendiacore.gui.widget.label.model.LabelRole;
import com.ascendiacore.gui.widget.list.ListBox;
import com.ascendiacore.gui.widget.list.ListRow;
import com.ascendiacore.gui.widget.list.model.RowStyle;
import com.ascendiacore.gui.widget.panel.model.FillKind;
import com.ascendiacore.gui.widget.panel.model.FillStyle;
import com.ascendiacore.gui.widget.scrollarea.ScrollArea;
import com.exoticworlds.shape.WorldLoopPresets;
import com.exoticworlds.shape.layout.DimensionLayout;
import com.exoticworlds.shape.layout.LayoutPreset;
import com.exoticworlds.shape.layout.WorldLayout;
import com.uber.nullaway.annotations.Initializer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class WorldShapeSettingsScreen extends Screen {
    private static final Theme THEME = Themes.VANILLA;
    private static final FillStyle FILL = THEME.panel().fills().of(FillKind.PARCHMENT);
    private static final int CONTENT_WIDTH = 468;
    private static final int SIDE_MARGIN = 10;
    private static final int BODY_GAP = 6;
    private static final int PRESET_GAP = 5;
    private static final int SPLIT_GAP = 8;
    private static final int LIST_MIN_WIDTH = 100;
    private static final int LIST_WIDTH = 150;
    private static final int OVERWORLD_ROW = 0;
    private static final int NETHER_ROW = 1;
    private static final int FOOTER_GAP = 8;
    private static final int FOOTER_BUTTON_WIDTH = 150;

    private final Screen parent;
    private final Consumer<WorldLayout> onDone;
    private WorldLayout chosen;
    private HeaderAndFooterLayout frame;
    private ComponentTree contents;
    private DimensionPage overworld;
    private DimensionPage nether;

    public WorldShapeSettingsScreen(Screen parent, WorldLayout current, Consumer<WorldLayout> onDone) {
        super(SettingsLines.TITLE);
        this.parent = parent;
        this.onDone = onDone;
        this.chosen = current;
    }

    @Override
    @Initializer
    protected void init() {
        this.overworld = new DimensionPage(DimensionOffer.OVERWORLD, THEME, FILL, this.parent, () -> null,
                this.chosen.overworld());
        this.nether = new DimensionPage(DimensionOffer.NETHER, THEME, FILL, this.parent,
                () -> this.chosen.overworld().spans(), this.chosen.nether());
        this.nether.showNetherScale(this.chosen.netherScale());
        this.frame = new HeaderAndFooterLayout(this);
        this.frame.addToHeader(new ComponentTree(header()));
        this.contents = this.frame.addToContents(new ComponentTree(this.body()));
        this.frame.addToFooter(new ComponentTree(this.footer()));
        this.frame.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        this.contents.setSize(Math.min(CONTENT_WIDTH, this.width - 2 * SIDE_MARGIN), this.frame.getContentHeight());
        this.frame.arrangeElements();
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(this.parent);
    }

    private static Column header() {
        Column header = new Column();
        header.add(Heading.of(FILL, SettingsLines.TITLE));
        return header;
    }

    private Column body() {
        Column body = new Column();
        body.setGap(BODY_GAP);
        body.add(this.presetRow());
        body.add(this.split(), Length.fill());
        return body;
    }

    private Row presetRow() {
        Row row = new Row();
        row.setGap(PRESET_GAP);
        for (WorldLoopPresets preset : WorldLoopPresets.values()) {
            Button button = Button.text(THEME.button(), SettingsLines.presetLabel(preset), () -> this.apply(preset));
            button.setSelected(() -> LayoutPreset.matches(this.current(), preset));
            button.setTooltip(THEME.tooltip(), SettingsLines.presetHint(preset));
            row.add(button, Length.fill());
        }

        return row;
    }

    private Row split() {
        ListBox dimensions = new ListBox(THEME, FILL, RowStyle.FRAMED, selected -> {
        });
        dimensions.addHeading(SettingsLines.VANILLA_DIMENSIONS);
        addRow(dimensions, SettingsLines.OVERWORLD,
                () -> SettingsLines.summary(this.current().overworld(), this.current().overworld().spans()));
        addRow(dimensions, SettingsLines.NETHER,
                () -> SettingsLines.summary(this.current().nether(), this.current().netherSpans()));
        addRow(dimensions, SettingsLines.END,
                () -> SettingsLines.summary(this.current().end(), this.current().endSpans()));

        Column pages = new Column();
        this.overworld.page().setVisible(() -> dimensions.selected() == OVERWORLD_ROW);
        pages.add(this.overworld.page());
        this.nether.page().setVisible(() -> dimensions.selected() == NETHER_ROW);
        pages.add(this.nether.page());

        Row split = new Row();
        split.setGap(SPLIT_GAP);
        split.add(dimensions, Length.flexible(LIST_MIN_WIDTH, LIST_WIDTH));
        split.add(new ScrollArea(THEME.scrollbar(), pages), Length.fill());
        return split;
    }

    private static void addRow(ListBox dimensions, Component name, Property<Component> summary) {
        ListRow row = dimensions.addRow(name);
        row.add(new Label(row.fill(), LabelRole.NORMAL, name));
        Label line = new Label(row.fill(), LabelRole.MUTED, Component.empty());
        line.setText(summary);
        row.add(line);
    }

    private Row footer() {
        Button done = Button.text(THEME.button(), CommonComponents.GUI_DONE, this::commit);
        done.setEnabled(() -> this.overworld.layout() != null && this.nether.layout() != null);
        Row footer = new Row();
        footer.setGap(FOOTER_GAP);
        footer.add(done, Length.fixed(FOOTER_BUTTON_WIDTH));
        footer.add(Button.text(THEME.button(), CommonComponents.GUI_CANCEL, this::onClose),
                Length.fixed(FOOTER_BUTTON_WIDTH));
        return footer;
    }

    private WorldLayout current() {
        DimensionLayout shown = this.overworld.layout();
        if (shown != null) {
            this.chosen = this.chosen.withOverworld(shown);
        }

        DimensionLayout shownNether = this.nether.layout();
        if (shownNether != null) {
            this.chosen = this.chosen.withNether(shownNether, this.nether.netherScale());
        }

        return this.chosen;
    }

    private void apply(WorldLoopPresets preset) {
        this.chosen = LayoutPreset.apply(this.current(), preset);
        this.overworld.show(this.chosen.overworld());
        this.nether.show(this.chosen.nether());
        this.nether.showNetherScale(this.chosen.netherScale());
    }

    private void commit() {
        this.onDone.accept(this.current());
        this.onClose();
    }
}
