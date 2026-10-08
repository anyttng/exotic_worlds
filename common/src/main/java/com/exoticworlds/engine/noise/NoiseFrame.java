package com.exoticworlds.engine.noise;

public record NoiseFrame(SlotAxes axes, double xDivisor, double zDivisor, double verticalShare) {
    public static final NoiseFrame UNDECLARED = new NoiseFrame(SlotAxes.DEFAULT, NoiseConstants.UNDIVIDED,
            NoiseConstants.UNDIVIDED, GenerationTransformerContext.UNDECLARED_VERTICAL_SHARE);

    boolean isDefault() {
        return this.axes == SlotAxes.DEFAULT
                && this.xDivisor == NoiseConstants.UNDIVIDED
                && this.zDivisor == NoiseConstants.UNDIVIDED;
    }
}
