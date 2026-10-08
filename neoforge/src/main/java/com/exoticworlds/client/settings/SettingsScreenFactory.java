package com.exoticworlds.client.settings;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// Inlined into WorldLoop, the lambda makes the verifier load Screen when WorldLoop links, and a dedicated server has none.
public final class SettingsScreenFactory {
    public static void register(ModContainer modContainer) {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                (container, modListScreen) -> new SettingsScreen(modListScreen));
    }

    private SettingsScreenFactory() {
    }
}
