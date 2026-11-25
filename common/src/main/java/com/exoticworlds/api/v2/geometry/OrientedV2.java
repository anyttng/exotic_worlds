package com.exoticworlds.api.v2.geometry;

/**
 * A moved value together with the {@link OrientationV2} the move applied to reach it. The record is always newly built,
 * and the {@code value} inside it is the argument instance itself where nothing moved.
 *
 * @param <T> the type of the moved value
 */
public record OrientedV2<T>(T value, OrientationV2 orientation) {

    /** Whether the move turned nothing. */
    public boolean isIdentity() {
        return this.orientation.isIdentity();
    }
}
