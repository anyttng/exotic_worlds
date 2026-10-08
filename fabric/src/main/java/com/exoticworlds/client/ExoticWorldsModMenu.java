package com.exoticworlds.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.exoticworlds.client.settings.SettingsScreen;

public class ExoticWorldsModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return SettingsScreen::new;
    }
}
