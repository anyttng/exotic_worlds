package com.exoticworlds.accessors;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.CarriedShape;

public interface ShapeStamp {
    default @Nullable CarriedShape toroidal$carriedShape() {
        return null;
    }

    default void toroidal$stamp(CarriedShape carried) {
    }

    default void toroidal$clearStamp() {
    }
}
