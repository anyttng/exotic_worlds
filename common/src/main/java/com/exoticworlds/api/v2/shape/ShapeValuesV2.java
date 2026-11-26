package com.exoticworlds.api.v2.shape;

/** The values a dimension holds for the parameters of its shape. */
public interface ShapeValuesV2 {
    /**
     * The value of {@code parameter} in this dimension. It is what the player chose, or the parameter's stored default
     * where the world never stated it.
     *
     * @throws IllegalStateException if {@code parameter} is not one the shape returns from {@link
     *        ShapeV2#getParameters()}
     */
    <T> T getValue(ParameterV2<T> parameter);
}
