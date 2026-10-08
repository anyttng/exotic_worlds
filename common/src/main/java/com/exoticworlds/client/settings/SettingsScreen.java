package com.exoticworlds.client.settings;

import java.util.List;
import java.util.Locale;

import com.mojang.serialization.Codec;
import com.exoticworlds.compat.MapCopies;
import com.exoticworlds.settings.Settings;
import com.exoticworlds.settings.SettingsService;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

public class SettingsScreen extends OptionsSubScreen {
    private static final String TITLE_KEY = "gui.exotic_worlds.settings.title";
    private static final String MAP_COPIES_KEY = "gui.exotic_worlds.settings.map_copies";
    private static final String HINT_SUFFIX = "_hint";

    private static final Codec<MapCopies> MAP_COPIES_CODEC = Codec.STRING.xmap(
            name -> MapCopies.fromName(name).orElse(Settings.DEFAULT_MAP_COPIES), MapCopies::name);

    private final OptionInstance<MapCopies> mapCopies;

    public SettingsScreen(Screen lastScreen) {
        super(lastScreen, Minecraft.getInstance().options, Component.translatable(TITLE_KEY));

        this.mapCopies = new OptionInstance<>(MAP_COPIES_KEY,
                OptionInstance.cachedConstantTooltip(Component.translatable(MAP_COPIES_KEY + HINT_SUFFIX)),
                (caption, value) -> label(value),
                new OptionInstance.Enum<>(List.of(MapCopies.values()), MAP_COPIES_CODEC),
                SettingsService.get().settings().mapCopies(),
                value -> SettingsService.get().update(new Settings(value)));
    }

    @Override
    protected void addOptions() {
        this.list.addBig(this.mapCopies);
    }

    // Vanilla's OptionsSubScreen rewrites options.txt here, and this screen owns no vanilla option.
    @Override
    public void removed() {
    }

    private static Component label(MapCopies copies) {
        return Component.translatable(MAP_COPIES_KEY + "." + copies.name().toLowerCase(Locale.ROOT));
    }
}
