package com.exoticworlds.accessors;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.CarriedShape;

public interface ShapeStamp {
    default @Nullable CarriedShape exotic_worlds$carriedShape() {
        return null;
    }

    default void exotic_worlds$stamp(CarriedShape carried) {
    }

    default void exotic_worlds$clearStamp() {
    }
}
