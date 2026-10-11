package com.exoticworlds.compat;

import java.util.Optional;

import com.exoticworlds.settings.SettingsService;

public enum MapCopies {
    REPEATED,
    SINGLE;

    public static MapCopies current() {
        return SettingsService.get().settings().mapCopies();
    }

    public boolean allowsSeamLines() {
        return this == REPEATED;
    }

    public boolean drawsSeamLines() {
        return allowsSeamLines() && SettingsService.get().settings().seamLines();
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
