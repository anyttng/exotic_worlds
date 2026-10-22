package com.exoticworlds.compat.arsnouveau.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.hollingsworth.arsnouveau.common.network.PacketTimedEvent;

import net.minecraft.nbt.CompoundTag;

@Mixin(value = PacketTimedEvent.class, remap = false)
public interface PacketTimedEventAccessor {
    @Accessor("tag")
    CompoundTag exotic_worlds$tag();
}
