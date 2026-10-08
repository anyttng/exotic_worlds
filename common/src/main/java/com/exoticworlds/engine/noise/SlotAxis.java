package com.exoticworlds.engine.noise;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WrapDomain;
import com.exoticworlds.engine.noise.GenerationTransformerContext.Context;

import net.minecraft.core.Direction;

public enum SlotAxis {
    X,
    Z,
    NONE;

    private static final WrapDomain UNWRAPPED = new WrapDomain.Noop();

    public boolean carriesWorldAxis() {
        return this != NONE;
    }

    public double samplerInput(double coord, double uniformScale) {
        return carriesWorldAxis() ? coord : coord * uniformScale;
    }

    public WrapDomain domainOf(WorldFold transformer) {
        return switch (this) {
            case X -> transformer.blockDomain(Direction.Axis.X);
            case Z -> transformer.blockDomain(Direction.Axis.Z);
            case NONE -> UNWRAPPED;
        };
    }

    public double divisorIn(Context context) {
        return switch (this) {
            case X -> context.xDivisor();
            case Z -> context.zDivisor();
            case NONE -> NoiseConstants.UNDIVIDED;
        };
    }
}
