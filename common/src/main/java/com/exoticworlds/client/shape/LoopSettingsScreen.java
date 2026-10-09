package com.exoticworlds.client.shape;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.client.WorldOptionContext;
import com.exoticworlds.api.v1.client.WorldOptionControl;
import com.exoticworlds.api.v1.client.WorldOptionControls;
import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.api.v1.option.WorldOption;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ScrollableLayout;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public abstract class LoopSettingsScreen<S> extends Screen {
    private static final int FOOTER_SPACING = 8;
    private static final int CONTENTS_SPACING = 8;
    private static final Component ADDITIONAL_SECTION =
            Component.translatable("gui.exotic_worlds.toroidal_settings.section.additional")
                    .withStyle(ChatFormatting.BOLD);

    protected final LoopSizeControls controls;

    private final Screen parent;
    private final Consumer<S> onDone;
    private final GenerationOptions generationOptions;
    private final Collection<? extends WorldOption<?>> offeredOptions;

    private @Nullable List<WorldOptionControl> optionControls;
    private HeaderAndFooterLayout layout;
    private ScrollableLayout contentsScroll;
    private Button doneButton;

    @SuppressWarnings("this-escape")
    protected LoopSettingsScreen(Component title, Screen parent, Function<Runnable, LoopSizeControls> controls,
            GenerationOptions generationOptions, Collection<? extends WorldOption<?>> offeredOptions,
            Consumer<S> onDone) {
        super(title);
        this.parent = parent;
        this.onDone = onDone;
        this.controls = controls.apply(this::onControlsChanged);
        this.generationOptions = generationOptions;
        this.offeredOptions = offeredOptions;
    }

    protected abstract S build();

    protected abstract OptionContext newOptionContext();

    protected void addBeforeFields(Font font, LinearLayout contents) {
    }

    protected void addAfterFields(Font font, LinearLayout contents) {
    }

    protected boolean isComplete() {
        return this.controls.isComplete() && this.optionControls().stream().allMatch(WorldOptionControl::isComplete);
    }

    protected void onControlsChanged() {
        this.refreshDoneButton();

        for (WorldOptionControl control : this.optionControls()) {
            control.onSharedStateChanged();
        }
    }

    protected final Screen parent() {
        return this.parent;
    }

    protected final GenerationOptions committedOptions() {
        GenerationOptions chosen = this.generationOptions;
        for (WorldOptionControl control : this.optionControls()) {
            chosen = control.commit(chosen);
        }

        return chosen;
    }

    protected final void refreshDoneButton() {
        this.doneButton.active = this.isComplete();
    }

    @Override
    protected void init() {
        this.layout = new HeaderAndFooterLayout(this);
        this.layout.addTitleHeader(this.title, this.font);

        LinearLayout contents = LinearLayout.vertical().spacing(CONTENTS_SPACING);
        this.controls.addPresets(contents);
        this.addBeforeFields(this.font, contents);
        this.controls.addFields(this.font, contents);
        this.addAfterFields(this.font, contents);
        this.addOptionControls(this.font, contents);

        this.contentsScroll = new ScrollableLayout(this.minecraft, contents, this.layout.getContentHeight());
        this.layout.addToContents(this.contentsScroll);

        LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(FOOTER_SPACING));
        this.doneButton = footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> this.commit()).build());
        footer.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose()).build());

        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
        this.controls.refresh();
    }

    @Override
    protected void repositionElements() {
        this.contentsScroll.arrangeElements();
        this.contentsScroll.setMaxHeight(this.layout.getContentHeight());
        this.layout.arrangeElements();
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.parent);
    }

    private void addOptionControls(Font font, LinearLayout contents) {
        List<WorldOptionControl> controls = this.optionControls();
        if (controls.isEmpty()) {
            return;
        }

        contents.addChild(new StringWidget(ADDITIONAL_SECTION, font), LayoutSettings::alignHorizontallyCenter);
        for (WorldOptionControl control : controls) {
            control.addWidgets(font, contents);
        }
    }

    private List<WorldOptionControl> optionControls() {
        if (this.optionControls == null) {
            this.optionControls = WorldOptionControls.createAll(this.newOptionContext(), this.offeredOptions);
        }

        return this.optionControls;
    }

    private void commit() {
        if (!this.isComplete()) {
            return;
        }

        this.onDone.accept(this.build());
        this.onClose();
    }

    protected abstract class OptionContext implements WorldOptionContext {
        @Override
        public Screen parent() {
            return LoopSettingsScreen.this.parent;
        }

        @Override
        public GenerationOptions options() {
            return LoopSettingsScreen.this.generationOptions;
        }

        @Override
        public void onChanged() {
            LoopSettingsScreen.this.refreshDoneButton();
        }

        @Override
        public void rebuild() {
            LoopSettingsScreen.this.rebuildWidgets();
        }
    }
}
