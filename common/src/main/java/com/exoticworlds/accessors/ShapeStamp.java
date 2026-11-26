package com.exoticworlds.accessors;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.world.CarriedShape;
import com.exoticworlds.core.world.DimensionTable;

public interface ShapeStamp {
    default @Nullable CarriedShape exotic_worlds$getCarriedShape() {
        return null;
    }

    default void exotic_worlds$stamp(CarriedShape carried) {
    }

    default @Nullable CarriedShape exotic_worlds$getBakedShape() {
        return null;
    }

    default void exotic_worlds$stampBaked(@Nullable CarriedShape carried) {
    }

    default DimensionTable exotic_worlds$getDimensionTable() {
        return DimensionTable.EMPTY;
    }

    default void exotic_worlds$stampDimensionTable(DimensionTable table) {
    }

    default void exotic_worlds$clearStamp() {
    }
}
