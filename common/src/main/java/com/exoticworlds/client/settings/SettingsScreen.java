package com.exoticworlds.client.settings;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

import com.ascendiacore.gui.base.component.model.TooltipStyle;
import com.ascendiacore.gui.base.hint.Hint;
import com.ascendiacore.gui.base.hint.model.HintValue;
import com.ascendiacore.gui.theme.Themes;
import com.mojang.serialization.Codec;
import com.exoticworlds.compat.map.MapCopies;
import com.exoticworlds.settings.Settings;
import com.exoticworlds.settings.SettingsService;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

public class SettingsScreen extends OptionsSubScreen {
    private static final String TITLE_KEY = "gui.exotic_worlds.settings.title";
    private static final String MAP_COPIES_KEY = "gui.exotic_worlds.settings.map_copies";
    private static final String SEAM_LINES_KEY = "gui.exotic_worlds.settings.seam_lines";
    private static final String MAP_COPIES_HINT_PREFIX = MAP_COPIES_KEY + ".hint.";
    private static final String SEAM_LINES_HINT_PREFIX = SEAM_LINES_KEY + ".hint.";
    private static final String INTRO = "intro";
    private static final String NOTE = "note";
    private static final String REQUIREMENT = "requirement";

    private static final Codec<MapCopies> MAP_COPIES_CODEC = Codec.STRING.xmap(
            name -> MapCopies.fromName(name).orElse(Settings.DEFAULT_MAP_COPIES), MapCopies::name);

    private final OptionInstance<MapCopies> mapCopies;
    private final OptionInstance<Boolean> seamLines;

    public SettingsScreen(Screen lastScreen) {
        super(lastScreen, Minecraft.getInstance().options, Component.translatable(TITLE_KEY));

        this.mapCopies = new OptionInstance<>(MAP_COPIES_KEY,
                chosen -> Tooltip.create(mapCopiesHint(chosen)),
                (caption, value) -> label(value),
                new OptionInstance.Enum<>(List.of(MapCopies.values()), MAP_COPIES_CODEC),
                SettingsService.get().getSettings().mapCopies(),
                value -> {
                    SettingsService.get().update(SettingsService.get().getSettings().withMapCopies(value));
                    this.offerSeamLinesFor(value);
                });
        this.seamLines = OptionInstance.createBoolean(SEAM_LINES_KEY,
                OptionInstance.cachedConstantTooltip(seamLinesHint()),
                SettingsService.get().getSettings().seamLines(),
                value -> SettingsService.get().update(SettingsService.get().getSettings().withSeamLines(value)));
    }

    @Override
    protected void addOptions() {
        OptionsList list = Objects.requireNonNull(this.list, "OptionsSubScreen.init builds the list first");
        list.addBig(this.mapCopies);
        list.addBig(this.seamLines);
        this.offerSeamLinesFor(this.mapCopies.get());
    }

    // Vanilla's OptionsSubScreen rewrites options.txt here, and this screen owns no vanilla option.
    @Override
    public void removed() {
    }

    private void offerSeamLinesFor(MapCopies copies) {
        AbstractWidget button = this.list == null ? null : this.list.findOption(this.seamLines);
        if (button != null) {
            button.active = copies.canDrawSeamLines();
        }
    }

    private static Component mapCopiesHint(MapCopies chosen) {
        return Hint.of(hintStyle())
                .intro(Component.translatable(MAP_COPIES_HINT_PREFIX + INTRO))
                .values(Stream.of(MapCopies.values())
                        .map(copies -> new HintValue(label(copies),
                                Component.translatable(MAP_COPIES_HINT_PREFIX + valueKey(copies)), copies == chosen))
                        .toList())
                .note(Component.translatable(MAP_COPIES_HINT_PREFIX + NOTE))
                .build();
    }

    private static Component seamLinesHint() {
        return Hint.of(hintStyle())
                .intro(Component.translatable(SEAM_LINES_HINT_PREFIX + INTRO))
                .requirement(Component.translatable(SEAM_LINES_HINT_PREFIX + REQUIREMENT,
                        label(MapCopies.REPEATED), label(MapCopies.SINGLE)))
                .build();
    }

    private static TooltipStyle hintStyle() {
        return Themes.get(Themes.VANILLA).tooltip();
    }

    private static Component label(MapCopies copies) {
        return Component.translatable(MAP_COPIES_KEY + "." + valueKey(copies));
    }

    private static String valueKey(MapCopies copies) {
        return copies.name().toLowerCase(Locale.ROOT);
    }
}
