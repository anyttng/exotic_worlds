package com.exoticworlds.engine.noise;

public record NoiseFrame(SlotAxes axes, double xDivisor, double zDivisor, double verticalShare,
        BoxDivisors boxDivisors) {
    public static final NoiseFrame UNDECLARED = new NoiseFrame(SlotAxes.DEFAULT, NoiseConstants.UNDIVIDED,
            NoiseConstants.UNDIVIDED, GenerationFoldContext.UNDECLARED_VERTICAL_SHARE);

    public NoiseFrame(SlotAxes axes, double xDivisor, double zDivisor, double verticalShare) {
        this(axes, xDivisor, zDivisor, verticalShare, BoxDivisors.UNDIVIDED);
    }

    public boolean isDefault() {
        return this.axes == SlotAxes.DEFAULT
                && this.xDivisor == NoiseConstants.UNDIVIDED
                && this.zDivisor == NoiseConstants.UNDIVIDED;
    }
}
