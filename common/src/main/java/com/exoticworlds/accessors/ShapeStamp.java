package com.exoticworlds.accessors;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.CarriedShape;
import com.exoticworlds.core.DimensionTable;

public interface ShapeStamp {
    default @Nullable CarriedShape exotic_worlds$carriedShape() {
        return null;
    }

    default void exotic_worlds$stamp(CarriedShape carried) {
    }

    default @Nullable CarriedShape exotic_worlds$bakedShape() {
        return null;
    }

    default void exotic_worlds$stampBaked(@Nullable CarriedShape carried) {
    }

    default DimensionTable exotic_worlds$dimensionTable() {
        return DimensionTable.EMPTY;
    }

    default void exotic_worlds$stampDimensionTable(DimensionTable table) {
    }

    default void exotic_worlds$clearStamp() {
    }
}
