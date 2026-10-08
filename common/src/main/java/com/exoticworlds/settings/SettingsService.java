package com.exoticworlds.settings;

import java.nio.file.Path;

import com.exoticworlds.ExoticWorlds;

public final class SettingsService {
    public static final String FILE_NAME = ExoticWorlds.MODID + ".json";

    private static SettingsService service;

    private final Path file;

    private volatile Settings settings;

    private SettingsService(Path file, Settings settings) {
        this.file = file;
        this.settings = settings;
    }

    public static SettingsService load(Path configDir) {
        Path file = configDir.resolve(FILE_NAME);
        return new SettingsService(file, SettingsFile.load(file));
    }

    public static void set(SettingsService chosen) {
        service = chosen;
    }

    public static SettingsService get() {
        if (service == null) {
            throw new IllegalStateException(
                    "Settings are not loaded - the loader's client entrypoint must call SettingsService.set first.");
        }

        return service;
    }

    public Settings settings() {
        return settings;
    }

    public void update(Settings updated) {
        if (updated.equals(settings)) {
            return;
        }

        settings = updated;
        SettingsFile.save(file, updated);
    }
}
