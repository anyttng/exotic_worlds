package com.exoticworlds.client.shape.layout;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.ascendiacore.client.gui.bridge.ComponentTree;
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
import com.exoticworlds.api.v1.shape.LoopSpans;
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
    private static final int FOOTER_GAP = 8;
    private static final int FOOTER_BUTTON_WIDTH = 150;

    private final Screen parent;
    private final List<ModStem> modDimensions;
    private final Consumer<WorldLayout> onDone;
    private final List<Slot> slots = new ArrayList<>();
    private WorldLayout chosen;
    private HeaderAndFooterLayout frame;
    private ComponentTree contents;

    @FunctionalInterface
    private interface Fold {
        WorldLayout apply(WorldLayout layout, DimensionPage page, DimensionLayout shown);
    }

    private record Slot(DimensionPage page, Function<WorldLayout, DimensionLayout> shown,
            Function<WorldLayout, @Nullable LoopSpans> spans, Fold fold, BiConsumer<DimensionPage, WorldLayout> extra) {
    }

    public WorldShapeSettingsScreen(Screen parent, WorldLayout current, List<ModStem> modDimensions,
            Consumer<WorldLayout> onDone) {
        super(SettingsLines.TITLE);
        this.parent = parent;
        this.modDimensions = List.copyOf(modDimensions);
        this.onDone = onDone;
        this.chosen = current;
    }

    @Override
    @Initializer
    protected void init() {
        this.fillSlots();
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

    private void fillSlots() {
        Supplier<@Nullable LoopSpans> overworldSpans = () -> this.chosen.overworld().spans();
        BiConsumer<DimensionPage, WorldLayout> nothing = (page, layout) -> {
        };
        this.slots.clear();
        this.slots.add(this.slot(DimensionOffer.OVERWORLD, overworldSpans, WorldLayout::overworld,
                layout -> layout.overworld().spans(), (layout, page, shown) -> layout.withOverworld(shown), nothing));
        this.slots.add(this.slot(DimensionOffer.NETHER, overworldSpans, WorldLayout::nether, WorldLayout::netherSpans,
                (layout, page, shown) -> layout.withNether(shown, page.netherScale()),
                (page, layout) -> page.showNetherScale(layout.netherScale())));
        this.slots.add(this.slot(DimensionOffer.END, overworldSpans, WorldLayout::end, WorldLayout::endSpans,
                (layout, page, shown) -> layout.withEnd(shown), nothing));
        for (ModStem stem : this.modDimensions) {
            this.slots.add(this.slot(DimensionOffer.modDimension(stem), overworldSpans,
                    layout -> layout.modDimension(stem.key()), stem::spans,
                    (layout, page, shown) -> layout.withModDimension(stem.key(), shown), nothing));
        }
    }

    private Slot slot(DimensionOffer offer, Supplier<@Nullable LoopSpans> overworldSpans,
            Function<WorldLayout, DimensionLayout> shown, Function<WorldLayout, @Nullable LoopSpans> spans, Fold fold,
            BiConsumer<DimensionPage, WorldLayout> extra) {
        DimensionPage page = new DimensionPage(offer, THEME, FILL, this.parent, overworldSpans, shown.apply(this.chosen));
        extra.accept(page, this.chosen);
        return new Slot(page, shown, spans, fold, extra);
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
        Column pages = new Column();
        for (int index = 0; index < this.slots.size(); index++) {
            this.addHeadingBefore(dimensions, index);
            this.addRow(dimensions, this.slots.get(index));
            int row = index;
            Column page = this.slots.get(index).page().page();
            page.setVisible(() -> dimensions.selected() == row);
            pages.add(page);
        }

        Row split = new Row();
        split.setGap(SPLIT_GAP);
        split.add(dimensions, Length.flexible(LIST_MIN_WIDTH, LIST_WIDTH));
        split.add(new ScrollArea(THEME.scrollbar(), pages), Length.fill());
        return split;
    }

    private void addHeadingBefore(ListBox dimensions, int index) {
        if (index == 0) {
            dimensions.addHeading(SettingsLines.VANILLA_DIMENSIONS);
        } else if (index == this.slots.size() - this.modDimensions.size()) {
            dimensions.addHeading(SettingsLines.MOD_DIMENSIONS);
        }
    }

    private void addRow(ListBox dimensions, Slot slot) {
        Component name = slot.page().title();
        ListRow row = dimensions.addRow(name);
        row.add(new Label(row.fill(), LabelRole.NORMAL, name));
        Label line = new Label(row.fill(), LabelRole.MUTED, Component.empty());
        line.setText(() -> this.summary(slot));
        row.add(line);
    }

    private Component summary(Slot slot) {
        WorldLayout layout = this.current();
        return SettingsLines.summary(slot.shown().apply(layout), slot.spans().apply(layout),
                slot.page().derivationKnown());
    }

    private Row footer() {
        Button done = Button.text(THEME.button(), CommonComponents.GUI_DONE, this::commit);
        done.setEnabled(() -> this.slots.stream().allMatch(slot -> slot.page().layout() != null));
        Row footer = new Row();
        footer.setGap(FOOTER_GAP);
        footer.add(done, Length.fixed(FOOTER_BUTTON_WIDTH));
        footer.add(Button.text(THEME.button(), CommonComponents.GUI_CANCEL, this::onClose),
                Length.fixed(FOOTER_BUTTON_WIDTH));
        return footer;
    }

    private WorldLayout current() {
        for (Slot slot : this.slots) {
            DimensionLayout shown = slot.page().layout();
            if (shown != null) {
                this.chosen = slot.fold().apply(this.chosen, slot.page(), shown);
            }
        }

        return this.chosen;
    }

    private void apply(WorldLoopPresets preset) {
        this.chosen = LayoutPreset.apply(this.current(), preset);
        for (Slot slot : this.slots) {
            slot.page().show(slot.shown().apply(this.chosen));
            slot.extra().accept(slot.page(), this.chosen);
        }
    }

    private void commit() {
        this.onDone.accept(this.current());
        this.onClose();
    }
}
