package com.exoticworlds.accessors;

public interface ClimateFieldMark {
    default boolean exotic_worlds$climateField() {
        return false;
    }

    default void exotic_worlds$markClimateField() {
    }
}
