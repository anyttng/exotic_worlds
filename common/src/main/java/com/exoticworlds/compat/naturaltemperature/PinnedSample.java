package com.exoticworlds.compat.naturaltemperature;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.level.levelgen.DensityFunction;

public final class PinnedSample {
    private static final ThreadLocal<Pin> PIN = ThreadLocal.withInitial(Pin::new);

    private final DensityFunction.FunctionContext context;

    private final ClimateLap lap;

    private final int blockZ;

    private PinnedSample(DensityFunction.FunctionContext context, ClimateLap lap, int blockZ) {
        this.context = context;
        this.lap = lap;
        this.blockZ = blockZ;
    }

    public static @Nullable PinnedSample enter(ClimateLap lap, int blockX, int blockY, int blockZ) {
        Pin pin = PIN.get();
        PinnedSample previous = pin.sample;
        pin.sample = new PinnedSample(new DensityFunction.SinglePointContext(blockX, blockY, blockZ), lap, blockZ);
        return previous;
    }

    public static void exit(@Nullable PinnedSample previous) {
        PIN.get().sample = previous;
    }

    public static @Nullable PinnedSample current() {
        return PIN.get().sample;
    }

    public static DensityFunction.FunctionContext at(DensityFunction.FunctionContext asked) {
        PinnedSample pinned = PIN.get().sample;
        return pinned != null ? pinned.context : asked;
    }

    public ClimateLap lap() {
        return this.lap;
    }

    public int blockZ() {
        return this.blockZ;
    }

    private static final class Pin {
        private @Nullable PinnedSample sample;
    }
}
