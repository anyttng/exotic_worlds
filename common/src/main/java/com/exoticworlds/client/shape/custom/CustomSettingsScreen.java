package com.exoticworlds.client.shape.custom;

import java.util.List;
import java.util.function.Consumer;

import com.uber.nullaway.annotations.Initializer;
import com.exoticworlds.client.shape.LoopSizeControls;
import com.exoticworlds.shape.custom.CustomSettings;
import com.exoticworlds.shape.custom.DimensionShape;
import com.exoticworlds.shape.custom.ModDimension;
import com.exoticworlds.shape.custom.StemKind;
import com.exoticworlds.shape.custom.VanillaDimension;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.ScrollableLayout;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.CommonLayouts;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class CustomSettingsScreen extends Screen {
    private static final Component TITLE = Component.translatable("gui.exotic_worlds.custom_settings.title");
    private static final Component VANILLA_SECTION =
            Component.translatable("gui.exotic_worlds.custom_settings.vanilla_dimensions")
                    .withStyle(ChatFormatting.BOLD);
    private static final Component MOD_SECTION =
            Component.translatable("gui.exotic_worlds.custom_settings.mod_dimensions")
                    .withStyle(ChatFormatting.BOLD);
    private static final Component CUSTOMIZE = Component.translatable("selectWorld.customizeType");
    private static final int CUSTOMIZE_WIDTH = 98;
    private static final int ROW_SPACING = 4;
    private static final int CONTENTS_SPACING = 8;
    private static final int FOOTER_SPACING = 8;

    private final Screen parent;
    private final List<ModDimension> modDimensions;
    private final Consumer<CustomSettings> onDone;
    private CustomSettings chosen;
    private HeaderAndFooterLayout layout;
    private ScrollableLayout scrollArea;

    public CustomSettingsScreen(Screen parent, CustomSettings current, List<ModDimension> modDimensions,
            Consumer<CustomSettings> onDone) {
        super(TITLE);
        this.parent = parent;
        this.modDimensions = List.copyOf(modDimensions);
        this.onDone = onDone;
        this.chosen = current;
    }

    @Override
    @Initializer
    protected void init() {
        this.layout = new HeaderAndFooterLayout(this);
        this.layout.addTitleHeader(this.title, this.font);

        LinearLayout contents = LinearLayout.vertical().spacing(CONTENTS_SPACING);
        contents.addChild(new StringWidget(VANILLA_SECTION, this.font), LayoutSettings::alignHorizontallyCenter);
        for (VanillaDimension dimension : VanillaDimension.values()) {
            contents.addChild(CommonLayouts.labeledElement(this.font, this.dimensionRow(dimension),
                    ShapeLabels.dimension(dimension)));
        }

        if (!this.modDimensions.isEmpty()) {
            contents.addChild(new StringWidget(MOD_SECTION, this.font), LayoutSettings::alignHorizontallyCenter);
            for (ModDimension dimension : this.modDimensions) {
                contents.addChild(CommonLayouts.labeledElement(this.font, this.modRow(dimension),
                        ShapeLabels.dimension(dimension)));
            }
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
        Minecraft.getInstance().gui.setScreen(this.parent);
    }

    private LinearLayout dimensionRow(VanillaDimension dimension) {
        LinearLayout row = LinearLayout.horizontal().spacing(ROW_SPACING);
        Button customize = Button.builder(CUSTOMIZE, button -> this.customize(dimension))
                .width(CUSTOMIZE_WIDTH)
                .build();
        StemKind kind = this.chosen.get(dimension).kind();
        row.addChild(CycleButton.builder(ShapeLabels::kind, kind)
                .withValues(StemKind.values())
                .displayOnlyValue()
                .withTooltip(picked -> Tooltip.create(ShapeLabels.kindHint(picked)))
                .create(0, 0, LoopSizeControls.FIELD_WIDTH - CUSTOMIZE_WIDTH - ROW_SPACING,
                        LoopSizeControls.FIELD_HEIGHT, ShapeLabels.dimension(dimension),
                        (button, picked) -> {
                            this.choose(dimension, picked);
                            customize.active = picked != StemKind.NONE;
                        }));
        customize.active = kind != StemKind.NONE;
        row.addChild(customize);
        return row;
    }

    private LinearLayout modRow(ModDimension dimension) {
        LinearLayout row = LinearLayout.horizontal().spacing(ROW_SPACING);
        Button customize = Button.builder(CUSTOMIZE, button -> this.customize(dimension))
                .width(CUSTOMIZE_WIDTH)
                .build();
        ModStemChoice choice = ModStemChoice.of(this.chosen.get(dimension));
        row.addChild(CycleButton.builder(ShapeLabels::choice, choice)
                .withValues(ModStemChoice.values())
                .displayOnlyValue()
                .withTooltip(picked -> Tooltip.create(ShapeLabels.choiceHint(picked)))
                .create(0, 0, LoopSizeControls.FIELD_WIDTH - CUSTOMIZE_WIDTH - ROW_SPACING,
                        LoopSizeControls.FIELD_HEIGHT, ShapeLabels.dimension(dimension),
                        (button, picked) -> {
                            this.choose(dimension, picked);
                            customize.active = isCustomizable(picked);
                        }));
        customize.active = isCustomizable(choice);
        row.addChild(customize);
        return row;
    }

    private static boolean isCustomizable(ModStemChoice choice) {
        return choice == ModStemChoice.TORUS || choice == ModStemChoice.CYLINDER;
    }

    private void choose(VanillaDimension dimension, StemKind kind) {
        this.chosen = this.chosen.with(dimension, this.chosen.get(dimension).withKind(kind, dimension));
    }

    private void choose(ModDimension dimension, ModStemChoice choice) {
        StemKind kind = choice.kind();
        if (kind == null) {
            this.chosen = this.chosen.with(dimension, null);
            return;
        }

        DimensionShape current = this.chosen.get(dimension);
        DimensionShape base = current != null ? current : new DimensionShape(null, CustomSettings.DEFAULT_OPTIONS);
        this.chosen = this.chosen.with(dimension, base.withKind(kind, dimension));
    }

    private void customize(VanillaDimension dimension) {
        DimensionShape current = this.chosen.get(dimension);
        Minecraft.getInstance().gui.setScreen(new DimensionShapeScreen(this, dimension, current,
                shape -> this.chosen = this.chosen.with(dimension, shape)));
    }

    private void customize(ModDimension dimension) {
        DimensionShape current = this.chosen.get(dimension);
        if (current == null) {
            return;
        }

        Minecraft.getInstance().gui.setScreen(new DimensionShapeScreen(this, dimension, current,
                shape -> this.chosen = this.chosen.with(dimension, shape)));
    }

    private void commit() {
        this.onDone.accept(this.chosen);
        this.onClose();
    }
}
