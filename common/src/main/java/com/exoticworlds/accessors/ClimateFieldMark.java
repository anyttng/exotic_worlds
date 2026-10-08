package com.exoticworlds.accessors;

public interface ClimateFieldMark {
    default boolean toroidal$climateField() {
        return false;
    }

    default void toroidal$markClimateField() {
    }
}
