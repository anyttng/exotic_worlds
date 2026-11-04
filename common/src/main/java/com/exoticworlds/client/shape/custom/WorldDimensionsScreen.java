package com.exoticworlds.client.shape.custom;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.uber.nullaway.annotations.Initializer;
import com.exoticworlds.core.DimensionTable;
import com.exoticworlds.shape.custom.CustomDimensions;
import com.exoticworlds.shape.custom.CustomStem;
import com.exoticworlds.shape.custom.DimensionShape;
import com.exoticworlds.shape.custom.ModDimension;
import com.exoticworlds.shape.custom.VanillaDimension;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ScrollableLayout;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.CommonLayouts;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

public class WorldDimensionsScreen extends Screen {
    private static final String KEY_PREFIX = "gui.exotic_worlds.world_dimensions.";
    private static final Component TITLE = Component.translatable(KEY_PREFIX + "title");
    private static final Component RESTART =
            Component.translatable(KEY_PREFIX + "restart").withStyle(ChatFormatting.GRAY);
    private static final Component OTHER_SHAPE = Component.translatable(KEY_PREFIX + "other");
    private static final Component LOCKED_VANILLA = Component.translatable(KEY_PREFIX + "locked.vanilla");
    private static final Component LOCKED_CHOSEN = Component.translatable(KEY_PREFIX + "locked.chosen");
    private static final Component LOCKED_GENERATED = Component.translatable(KEY_PREFIX + "locked.generated");
    private static final Component VANILLA_SECTION =
            Component.translatable("gui.exotic_worlds.custom_settings.vanilla_dimensions")
                    .withStyle(ChatFormatting.BOLD);
    private static final Component MOD_SECTION =
            Component.translatable("gui.exotic_worlds.custom_settings.mod_dimensions")
                    .withStyle(ChatFormatting.BOLD);
    private static final int CONTENTS_SPACING = 8;
    private static final int FOOTER_SPACING = 8;

    private final List<Row> rows;
    private final Consumer<Map<ResourceKey<LevelStem>, DimensionTable.Entry>> onDone;
    private final Map<ResourceKey<LevelStem>, @Nullable DimensionShape> chosen = new HashMap<>();
    private HeaderAndFooterLayout layout;
    private ScrollableLayout scrollArea;

    public enum Lock {
        VANILLA,
        CHOSEN,
        GENERATED
    }

    public record Row(ResourceKey<LevelStem> stem, @Nullable Lock lock, DimensionTable.Entry entry) {
    }

    public WorldDimensionsScreen(List<Row> rows, Consumer<Map<ResourceKey<LevelStem>, DimensionTable.Entry>> onDone) {
        super(TITLE);
        this.rows = List.copyOf(rows);
        this.onDone = onDone;
        for (Row row : this.rows) {
            if (row.lock() == null) {
                this.chosen.put(row.stem(), null);
            }
        }
    }

    @Override
    @Initializer
    protected void init() {
        this.layout = new HeaderAndFooterLayout(this);
        this.layout.addTitleHeader(this.title, this.font);

        LinearLayout contents = LinearLayout.vertical().spacing(CONTENTS_SPACING);
        contents.addChild(new StringWidget(RESTART, this.font), LayoutSettings::alignHorizontallyCenter);
        contents.addChild(new StringWidget(VANILLA_SECTION, this.font), LayoutSettings::alignHorizontallyCenter);
        boolean modSection = false;
        for (Row row : this.rows) {
            CustomStem dimension = stemOf(row.stem());
            if (dimension instanceof ModDimension && !modSection) {
                contents.addChild(new StringWidget(MOD_SECTION, this.font), LayoutSettings::alignHorizontallyCenter);
                modSection = true;
            }

            contents.addChild(CommonLayouts.labeledElement(this.font, this.row(row, dimension),
                    ShapeLabels.dimension(dimension)));
        }

        this.scrollArea = this.layout.addToContents(
                new ScrollableLayout(this.minecraft, contents, this.layout.getContentHeight()));

        LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(FOOTER_SPACING));
        footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> this.commit()).build());
        footer.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose()).build());

        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        this.scrollArea.arrangeElements();
        this.scrollArea.setMaxHeight(this.layout.getContentHeight());
        this.layout.arrangeElements();
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(null);
    }

    private LayoutElement row(Row row, CustomStem dimension) {
        ResourceKey<LevelStem> key = row.stem();
        Lock lock = row.lock();
        if (lock == null && dimension instanceof ModDimension mod) {
            return ShapeRows.modRow(mod, ModStemChoice.of(this.chosen.get(key)),
                    picked -> this.chosen.put(key, picked.applyTo(this.chosen.get(key), mod)),
                    () -> this.customize(mod));
        }

        return ShapeRows.lockedRow(valueOf(row.entry()), reasonOf(lock == null ? Lock.VANILLA : lock));
    }

    private static Component valueOf(DimensionTable.Entry entry) {
        if (entry.fromOverworld()) {
            return ShapeLabels.choice(ModStemChoice.FROM_OVERWORLD);
        }

        DimensionShape shape = CustomDimensions.shapeOf(entry);
        return shape == null ? OTHER_SHAPE : ShapeLabels.kind(shape.kind());
    }

    private static Component reasonOf(Lock lock) {
        return switch (lock) {
            case VANILLA -> LOCKED_VANILLA;
            case CHOSEN -> LOCKED_CHOSEN;
            case GENERATED -> LOCKED_GENERATED;
        };
    }

    private void customize(ModDimension dimension) {
        DimensionShape current = this.chosen.get(dimension.stem());
        if (current == null) {
            return;
        }

        Minecraft.getInstance().setScreen(new DimensionShapeScreen(this, dimension, current,
                shape -> this.chosen.put(dimension.stem(), shape)));
    }

    private void commit() {
        Map<ResourceKey<LevelStem>, DimensionTable.Entry> choices = new HashMap<>();
        this.chosen.forEach((key, shape) -> choices.put(key, CustomDimensions.entryOf(shape)));
        if (!choices.isEmpty()) {
            this.onDone.accept(choices);
        }

        this.onClose();
    }

    private static CustomStem stemOf(ResourceKey<LevelStem> key) {
        for (VanillaDimension vanilla : VanillaDimension.values()) {
            if (vanilla.stem() == key) {
                return vanilla;
            }
        }

        return new ModDimension(key);
    }
}
