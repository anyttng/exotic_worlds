package com.exoticworlds.compat.electroenergetics.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.george_vi.electroenergetics.foundation.nodes.InWorldNode;
import com.george_vi.electroenergetics.simulation.SendVoltageDataPacket;

@Mixin(value = SendVoltageDataPacket.class, remap = false)
public interface SendVoltageDataPacketAccessor {
    @Accessor("nodes")
    InWorldNode[] toroidal$nodes();

    @Accessor("nodes")
    void toroidal$setNodes(InWorldNode[] nodes);

    @Accessor("voltages")
    double[] toroidal$voltages();

    @Accessor("voltages")
    void toroidal$setVoltages(double[] voltages);

    @Accessor("microTicks")
    int toroidal$microTicks();

    @Accessor("microTicks")
    void toroidal$setMicroTicks(int microTicks);

    @Accessor("frequencies")
    float[] toroidal$frequencies();

    @Accessor("frequencies")
    void toroidal$setFrequencies(float[] frequencies);
}
