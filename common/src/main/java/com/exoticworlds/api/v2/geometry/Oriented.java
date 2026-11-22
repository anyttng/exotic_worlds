package com.exoticworlds.api.v2.geometry;

/**
 * A moved value together with the {@link Orientation} the move applied to reach it. The record is always newly built;
 * the {@code value} inside it is the argument instance itself where nothing moved.
 *
 * @param <T> the type of the moved value
 */
public record Oriented<T>(T value, Orientation orientation) {

    /** Whether the move turned nothing. */
    public boolean isIdentity() {
        return this.orientation.isIdentity();
    }
}
