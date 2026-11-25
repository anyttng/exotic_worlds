package com.exoticworlds.compat.apprenticecodex.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import jp.aquafactory.apprenticecodex.network.packet.DeepSensorObservationsPacket;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

@Mixin(value = DeepSensorObservationsPacket.class, remap = false)
public interface DeepSensorObservationsPacketAccessor {
    @Accessor("dimension")
    ResourceKey<Level> exotic_worlds$getDimension();

    @Accessor("observations")
    List<DeepSensorObservationsPacket.Observation> exotic_worlds$getObservations();
}
