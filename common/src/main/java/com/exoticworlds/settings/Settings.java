package com.exoticworlds.settings;

import com.exoticworlds.compat.MapCopies;

public record Settings(MapCopies mapCopies) {
    public static final MapCopies DEFAULT_MAP_COPIES = MapCopies.REPEATED;

    public static Settings defaults() {
        return new Settings(DEFAULT_MAP_COPIES);
    }
}
