package com.exoticworlds.client.settings;

import java.util.List;
import java.util.Locale;

import com.mojang.serialization.Codec;
import com.exoticworlds.compat.MapCopies;
import com.exoticworlds.settings.Settings;
import com.exoticworlds.settings.SettingsService;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

public class SettingsScreen extends OptionsSubScreen {
    private static final String TITLE_KEY = "gui.exotic_worlds.settings.title";
    private static final String MAP_COPIES_KEY = "gui.exotic_worlds.settings.map_copies";
    private static final String SEAM_LINES_KEY = "gui.exotic_worlds.settings.seam_lines";
    private static final String HINT_SUFFIX = "_hint";

    private static final Codec<MapCopies> MAP_COPIES_CODEC = Codec.STRING.xmap(
            name -> MapCopies.fromName(name).orElse(Settings.DEFAULT_MAP_COPIES), MapCopies::name);

    private final OptionInstance<MapCopies> mapCopies;
    private final OptionInstance<Boolean> seamLines;

    public SettingsScreen(Screen lastScreen) {
        super(lastScreen, Minecraft.getInstance().options, Component.translatable(TITLE_KEY));

        this.mapCopies = new OptionInstance<>(MAP_COPIES_KEY,
                OptionInstance.cachedConstantTooltip(Component.translatable(MAP_COPIES_KEY + HINT_SUFFIX)),
                (caption, value) -> label(value),
                new OptionInstance.Enum<>(List.of(MapCopies.values()), MAP_COPIES_CODEC),
                SettingsService.get().settings().mapCopies(),
                value -> {
                    SettingsService.get().update(SettingsService.get().settings().withMapCopies(value));
                    this.offerSeamLinesFor(value);
                });
        this.seamLines = OptionInstance.createBoolean(SEAM_LINES_KEY,
                OptionInstance.cachedConstantTooltip(Component.translatable(SEAM_LINES_KEY + HINT_SUFFIX)),
                SettingsService.get().settings().seamLines(),
                value -> SettingsService.get().update(SettingsService.get().settings().withSeamLines(value)));
    }

    @Override
    protected void addOptions() {
        this.list.addBig(this.mapCopies);
        this.list.addBig(this.seamLines);
        this.offerSeamLinesFor(this.mapCopies.get());
    }

    // Vanilla's OptionsSubScreen rewrites options.txt here, and this screen owns no vanilla option.
    @Override
    public void removed() {
    }

    private void offerSeamLinesFor(MapCopies copies) {
        AbstractWidget button = this.list == null ? null : this.list.findOption(this.seamLines);
        if (button != null) {
            button.active = copies.allowsSeamLines();
        }
    }

    private static Component label(MapCopies copies) {
        return Component.translatable(MAP_COPIES_KEY + "." + copies.name().toLowerCase(Locale.ROOT));
    }
}
