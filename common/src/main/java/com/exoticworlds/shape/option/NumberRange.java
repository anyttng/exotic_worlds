package com.exoticworlds.shape.option;

public record NumberRange(int min, int max) {
    public NumberRange {
        if (min > max) {
            throw new IllegalArgumentException("A number range needs min <= max, got " + min + " and " + max);
        }
    }
}
