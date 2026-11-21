package com.exoticworlds.engine.noise;

public record BoxDivisors(double a, double b, double c) {
    public static final BoxDivisors UNDIVIDED = new BoxDivisors(NoiseConstants.UNDIVIDED, NoiseConstants.UNDIVIDED,
            NoiseConstants.UNDIVIDED);

    public boolean isUndivided() {
        return this.a == NoiseConstants.UNDIVIDED
                && this.b == NoiseConstants.UNDIVIDED
                && this.c == NoiseConstants.UNDIVIDED;
    }
}
