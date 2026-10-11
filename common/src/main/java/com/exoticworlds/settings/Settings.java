package com.exoticworlds.settings;

import com.exoticworlds.compat.MapCopies;

public record Settings(MapCopies mapCopies, boolean seamLines) {
    public static final MapCopies DEFAULT_MAP_COPIES = MapCopies.REPEATED;
    public static final boolean DEFAULT_SEAM_LINES = true;

    public static Settings defaults() {
        return new Settings(DEFAULT_MAP_COPIES, DEFAULT_SEAM_LINES);
    }

    public Settings withMapCopies(MapCopies chosen) {
        return new Settings(chosen, this.seamLines);
    }

    public Settings withSeamLines(boolean chosen) {
        return new Settings(this.mapCopies, chosen);
    }
}
