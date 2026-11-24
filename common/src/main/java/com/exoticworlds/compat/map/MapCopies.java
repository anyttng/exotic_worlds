package com.exoticworlds.compat.map;

import java.util.Optional;

import com.exoticworlds.settings.SettingsService;

public enum MapCopies {
    REPEATED,
    SINGLE;

    public static MapCopies getCurrent() {
        return SettingsService.get().getSettings().mapCopies();
    }

    public boolean canDrawSeamLines() {
        return this == REPEATED;
    }

    public boolean isDrawingSeamLines() {
        return canDrawSeamLines() && SettingsService.get().getSettings().seamLines();
    }

    public static Optional<MapCopies> fromName(String name) {
        for (MapCopies copies : values()) {
            if (copies.name().equalsIgnoreCase(name.strip())) {
                return Optional.of(copies);
            }
        }

        return Optional.empty();
    }
}
