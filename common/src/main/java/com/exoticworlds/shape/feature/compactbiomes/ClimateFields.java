package com.exoticworlds.shape.feature.compactbiomes;

import java.util.Set;

import com.exoticworlds.core.gen.RouterField;

public final class ClimateFields {
    public static boolean isClimate(Set<RouterField> readUnder) {
        return readUnder.contains(RouterField.TEMPERATURE) || readUnder.contains(RouterField.VEGETATION);
    }

    private ClimateFields() {
    }
}
