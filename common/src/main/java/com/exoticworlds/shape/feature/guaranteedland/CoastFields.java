package com.exoticworlds.shape.feature.guaranteedland;

import java.util.Set;

import com.exoticworlds.core.gen.RouterField;

public final class CoastFields {
    public static boolean isCoast(Set<RouterField> readUnder) {
        return readUnder.contains(RouterField.CONTINENTS);
    }

    private CoastFields() {
    }
}
