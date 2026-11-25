package com.exoticworlds.client.shape.layout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.ascendiacore.gui.widget.label.model.TextAlign;
import com.ascendiacore.gui.widget.list.ListBox;
import com.ascendiacore.gui.widget.list.ListRow;
import com.ascendiacore.gui.widget.list.model.RowStyle;
import com.ascendiacore.gui.widget.panel.model.FillKind;
import com.ascendiacore.gui.widget.panel.model.FillStyle;
import com.ascendiacore.gui.widget.scrollarea.ScrollArea;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.shape.WorldLoopPresets;
import com.exoticworlds.shape.layout.DimensionLayout;
import com.exoticworlds.shape.layout.LayoutPreset;
import com.exoticworlds.shape.layout.ShapeKind;
import com.exoticworlds.shape.layout.WorldLayout;
import com.uber.nullaway.annotations.Initializer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

public final class WorldShapeSettingsScreen extends Screen {
    private static final int CONTENT_WIDTH_PIXELS = 468;
    private static final int SIDE_MARGIN_PIXELS = 10;
    private static final int BODY_GAP_PIXELS = 6;
    private static final int PRESET_GAP_PIXELS = 5;
    private static final int SPLIT_GAP_PIXELS = 8;
    private static final int LIST_MIN_WIDTH_PIXELS = 100;
    private static final int LIST_WIDTH_PIXELS = 150;
    private static final int FOOTER_GAP_PIXELS = 8;
    private static final int FOOTER_BUTTON_WIDTH_PIXELS = 150;

    private final @Nullable Screen parent;
    private final List<ResourceKey<LevelStem>> modDimensions;
    private final CoordinateScales scales;
    private final Map<ResourceKey<LevelStem>, DimensionLock> locks;
    private final boolean isExistingWorld;
    private final Consumer<WorldLayout> onDone;
    private final List<Slot> slots = new ArrayList<>();
    private WorldLayout chosen;
    private HeaderAndFooterLayout frame;
    private ComponentTree contents;
    private @Nullable Roots roots;
    private Theme theme;
    private FillStyle fill;

    private record Roots(Column header, Column body, Row footer) {
    }

    @FunctionalInterface
    private interface Fold {
        WorldLayout apply(WorldLayout layout, DimensionPage page, DimensionLayout shown);
    }

    private record Slot(DimensionPage page, boolean isLocked, Function<WorldLayout, DimensionLayout> shown,
            Function<WorldLayout, @Nullable FlatShape> spans, Fold fold, BiConsumer<DimensionPage, WorldLayout> extra) {
    }

    private WorldShapeSettingsScreen(@Nullable Screen parent, WorldLayout current,
            List<ResourceKey<LevelStem>> modDimensions, CoordinateScales scales,
            Map<ResourceKey<LevelStem>, DimensionLock> locks, boolean isExistingWorld, Consumer<WorldLayout> onDone) {
        super(SettingsLines.TITLE);
        this.parent = parent;
        this.modDimensions = List.copyOf(modDimensions);
        this.scales = scales;
        this.locks = Map.copyOf(locks);
        this.isExistingWorld = isExistingWorld;
        this.onDone = onDone;
        this.chosen = current;
    }

    public static WorldShapeSettingsScreen forNewWorld(Screen parent, WorldLayout current,
            List<ResourceKey<LevelStem>> modDimensions, CoordinateScales scales, Consumer<WorldLayout> onDone) {
        return new WorldShapeSettingsScreen(parent, current, modDimensions, scales, Map.of(), false, onDone);
    }

    public static WorldShapeSettingsScreen forExistingWorld(WorldLayout current,
            List<ResourceKey<LevelStem>> modDimensions, CoordinateScales scales,
            Map<ResourceKey<LevelStem>, DimensionLock> locks, Consumer<WorldLayout> onDone) {
        return new WorldShapeSettingsScreen(null, current, modDimensions, scales, locks, true, onDone);
    }

    public @Nullable Screen getCreationScreen() {
        return this.parent;
    }

    @Override
    @Initializer
    protected void init() {
        this.theme = Themes.get(Themes.VANILLA);
        this.fill = this.theme.panel().fills().getFill(FillKind.PARCHMENT);
        Roots built = this.roots != null ? this.roots : this.buildRoots();
        this.roots = built;
        this.frame = new HeaderAndFooterLayout(this);
        this.frame.addToHeader(new ComponentTree(built.header()));
        this.contents = this.frame.addToContents(new ComponentTree(built.body()));
        this.frame.addToFooter(new ComponentTree(built.footer()));
        this.frame.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
    }

    private Roots buildRoots() {
        this.fillSlots();
        return new Roots(this.header(), this.body(), this.footer());
    }

    @Override
    protected void repositionElements() {
        this.contents.setSize(Math.min(CONTENT_WIDTH_PIXELS, this.width - 2 * SIDE_MARGIN_PIXELS), this.frame.getContentHeight());
        this.frame.arrangeElements();
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(this.parent);
    }

    private void fillSlots() {
        BiConsumer<DimensionPage, WorldLayout> nothing = (page, layout) -> {
        };
        this.slots.add(this.slot(DimensionOffer.OVERWORLD, LevelStem.OVERWORLD, WorldLayout::overworld,
                layout -> layout.overworld().spans(), (layout, page, shown) -> layout.withOverworld(shown), nothing));
        this.slots.add(this.slot(DimensionOffer.NETHER, LevelStem.NETHER, WorldLayout::nether, WorldLayout::getNetherSpans,
                (layout, page, shown) -> layout.withNether(shown, page.getNetherScale()),
                (page, layout) -> page.showNetherScale(layout.netherScale())));
        this.slots.add(this.slot(DimensionOffer.END, LevelStem.END, WorldLayout::end, WorldLayout::getEndSpans,
                (layout, page, shown) -> layout.withEnd(shown), nothing));
        for (ResourceKey<LevelStem> key : this.modDimensions) {
            this.slots.add(this.slot(DimensionOffer.modDimension(key, this.scales), key,
                    layout -> layout.getModDimension(key), layout -> this.modSpans(layout, key),
                    (layout, page, shown) -> layout.withModDimension(key, shown), nothing));
        }
    }

    private @Nullable FlatShape modSpans(WorldLayout layout, ResourceKey<LevelStem> key) {
        DimensionLayout chosen = layout.getModDimension(key);
        return chosen.kind() == ShapeKind.FROM_OVERWORLD
                ? this.scales.modDimension(key, layout.overworld().spans())
                : chosen.spans();
    }

    private Slot slot(DimensionOffer offer, ResourceKey<LevelStem> key, Function<WorldLayout, DimensionLayout> shown,
            Function<WorldLayout, @Nullable FlatShape> spans, Fold fold, BiConsumer<DimensionPage, WorldLayout> extra) {
        Supplier<DimensionLayout> overworld = () -> this.chosen.overworld();
        DimensionLock lock = this.locks.get(key);
        DimensionPage page = new DimensionPage(offer, this.theme, this.fill, this.scales, overworld, shown.apply(this.chosen),
                lock == null ? null : SettingsLines.lockReason(lock));
        extra.accept(page, this.chosen);
        return new Slot(page, lock != null, shown, spans, fold, extra);
    }

    private Column header() {
        Column header = new Column();
        header.add(Heading.of(this.fill, SettingsLines.TITLE));
        if (this.isExistingWorld) {
            Label note = new Label(this.fill, LabelRole.MUTED, SettingsLines.EXISTING_WORLD);
            note.setAlign(TextAlign.CENTER);
            header.add(note);
        }

        return header;
    }

    private Column body() {
        Column body = new Column();
        body.setGapPixels(BODY_GAP_PIXELS);
        body.add(this.presetRow());
        body.add(this.split(), Length.fill());
        return body;
    }

    private Row presetRow() {
        Row row = new Row();
        row.setGapPixels(PRESET_GAP_PIXELS);
        row.setEnabled(() -> !this.isExistingWorld && LayoutPreset.isOffered(this.getCurrent()));
        for (WorldLoopPresets preset : WorldLoopPresets.values()) {
            Button button = Button.text(this.theme.button(), SettingsLines.presetLabel(preset), () -> this.apply(preset));
            button.setSelected(() -> LayoutPreset.isMatching(this.getCurrent(), preset));
            button.setTooltip(this.theme.tooltip(), SettingsLines.presetHint(this.theme.tooltip(), preset));
            row.add(button, Length.fill());
        }

        return row;
    }

    private Row split() {
        ListBox dimensions = new ListBox(this.theme, this.fill, RowStyle.FRAMED, selected -> {
        });
        Column pages = new Column();
        List<Slot> listed = new ArrayList<>();
        Map<Integer, Component> headings = new HashMap<>();
        int vanilla = this.slots.size() - this.modDimensions.size();
        list(listed, headings, SettingsLines.UNDECIDED_DIMENSIONS, this.slots.stream().filter(this::isOpen).toList());
        list(listed, headings, SettingsLines.VANILLA_DIMENSIONS, this.decided(this.slots.subList(0, vanilla)));
        list(listed, headings, SettingsLines.MOD_DIMENSIONS,
                this.decided(this.slots.subList(vanilla, this.slots.size())));
        for (int index = 0; index < listed.size(); index++) {
            Component heading = headings.get(index);
            if (heading != null) {
                dimensions.addHeading(heading);
            }

            this.addRow(dimensions, listed.get(index));
            int row = index;
            Column page = listed.get(index).page().getPage();
            page.setVisible(() -> dimensions.getSelectedIndex() == row);
            pages.add(page);
        }

        Row split = new Row();
        split.setGapPixels(SPLIT_GAP_PIXELS);
        split.add(dimensions, Length.flexible(LIST_MIN_WIDTH_PIXELS, LIST_WIDTH_PIXELS));
        split.add(new ScrollArea(this.theme.scrollbar(), pages), Length.fill());
        return split;
    }

    private static void list(List<Slot> listed, Map<Integer, Component> headings, Component heading,
            List<Slot> group) {
        if (!group.isEmpty()) {
            headings.put(listed.size(), heading);
            listed.addAll(group);
        }
    }

    private boolean isOpen(Slot slot) {
        return this.isExistingWorld && !slot.isLocked();
    }

    private List<Slot> decided(List<Slot> group) {
        return group.stream().filter(slot -> !this.isOpen(slot)).toList();
    }

    private void addRow(ListBox dimensions, Slot slot) {
        Component name = slot.page().getTitle();
        ListRow row = dimensions.addRow(name);
        row.add(new Label(row.getFill(), slot.isLocked() ? LabelRole.MUTED : LabelRole.NORMAL, name));
        Label line = new Label(row.getFill(), LabelRole.MUTED, Component.empty());
        line.setText(() -> this.summary(slot));
        row.add(line);
    }

    private Component summary(Slot slot) {
        WorldLayout layout = this.getCurrent();
        return SettingsLines.summary(slot.shown().apply(layout), slot.spans().apply(layout),
                slot.page().isDerivationKnown());
    }

    private Row footer() {
        Button done = Button.text(this.theme.button(), CommonComponents.GUI_DONE, this::commit);
        done.setEnabled(() -> this.slots.stream().allMatch(slot -> slot.isLocked() || slot.page().layout() != null));
        Row footer = new Row();
        footer.setGapPixels(FOOTER_GAP_PIXELS);
        footer.add(done, Length.fixed(FOOTER_BUTTON_WIDTH_PIXELS));
        footer.add(Button.text(this.theme.button(), CommonComponents.GUI_CANCEL, this::onClose),
                Length.fixed(FOOTER_BUTTON_WIDTH_PIXELS));
        return footer;
    }

    private WorldLayout getCurrent() {
        for (Slot slot : this.slots) {
            DimensionLayout shown = slot.isLocked() ? null : slot.page().layout();
            if (shown != null) {
                this.chosen = slot.fold().apply(this.chosen, slot.page(), shown);
            }
        }

        return this.chosen;
    }

    private void apply(WorldLoopPresets preset) {
        this.chosen = LayoutPreset.apply(this.getCurrent(), preset, this.scales.of(LevelStem.OVERWORLD));
        for (Slot slot : this.slots) {
            slot.page().show(slot.shown().apply(this.chosen));
            slot.extra().accept(slot.page(), this.chosen);
        }
    }

    private void commit() {
        this.onDone.accept(this.getCurrent());
        this.onClose();
    }
}
