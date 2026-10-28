package com.exoticworlds.compat.naturaltemperature;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.level.levelgen.DensityFunction;

public final class PinnedSample {
    private static final ThreadLocal<Pin> PIN = ThreadLocal.withInitial(Pin::new);

    public static DensityFunction.@Nullable FunctionContext enter(int blockX, int blockY, int blockZ) {
        Pin pin = PIN.get();
        DensityFunction.@Nullable FunctionContext previous = pin.context;
        pin.context = new DensityFunction.SinglePointContext(blockX, blockY, blockZ);
        return previous;
    }

    public static void exit(DensityFunction.@Nullable FunctionContext previous) {
        PIN.get().context = previous;
    }

    public static DensityFunction.FunctionContext at(DensityFunction.FunctionContext asked) {
        DensityFunction.@Nullable FunctionContext pinned = PIN.get().context;
        return pinned != null ? pinned : asked;
    }

    private static final class Pin {
        private DensityFunction.@Nullable FunctionContext context;
    }

    private PinnedSample() {
    }
}
