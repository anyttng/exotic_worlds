package com.exoticworlds.compat.ultraterraforged;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.compat.terraforged.RtfLap;

import net.minecraft.core.Direction;

public final class CenterAxis implements AutoCloseable {
    private static final ThreadLocal<CenterAxis> CURRENT = ThreadLocal.withInitial(CenterAxis::new);

    private Direction.@Nullable Axis axis;
    private float xQuotient;
    private float zQuotient;

    public static CenterAxis current() {
        return CURRENT.get();
    }

    public CenterAxis along(Direction.Axis axis) {
        this.axis = axis;
        return this;
    }

    public float snapped(float frequency) {
        return this.axis == null ? frequency : RtfLap.snapped(this.axis, frequency);
    }

    public float keep(float quotient) {
        if (this.axis == Direction.Axis.X) {
            this.xQuotient = quotient;
        } else if (this.axis == Direction.Axis.Z) {
            this.zQuotient = quotient;
        }

        return quotient;
    }

    public float xQuotient() {
        return this.xQuotient;
    }

    public float zQuotient() {
        return this.zQuotient;
    }

    @Override
    public void close() {
        this.axis = null;
    }

    private CenterAxis() {
    }
}
