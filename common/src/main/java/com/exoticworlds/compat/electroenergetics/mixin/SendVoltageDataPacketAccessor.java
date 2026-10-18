package com.exoticworlds.compat.electroenergetics.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.george_vi.electroenergetics.foundation.nodes.InWorldNode;
import com.george_vi.electroenergetics.simulation.SendVoltageDataPacket;

@Mixin(value = SendVoltageDataPacket.class, remap = false)
public interface SendVoltageDataPacketAccessor {
    @Accessor("nodes")
    InWorldNode[] exotic_worlds$nodes();

    @Accessor("nodes")
    void exotic_worlds$setNodes(InWorldNode[] nodes);

    @Accessor("voltages")
    double[] exotic_worlds$voltages();

    @Accessor("voltages")
    void exotic_worlds$setVoltages(double[] voltages);

    @Accessor("microTicks")
    int exotic_worlds$microTicks();

    @Accessor("microTicks")
    void exotic_worlds$setMicroTicks(int microTicks);

    @Accessor("frequencies")
    float[] exotic_worlds$frequencies();

    @Accessor("frequencies")
    void exotic_worlds$setFrequencies(float[] frequencies);
}
