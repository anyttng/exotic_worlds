package com.exoticworlds.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.WorldBorder;

@Mixin(WorldBorder.class)
public interface WorldBorderAccessor {
    @Invoker("getListeners")
    List<BorderChangeListener> exotic_worlds$listeners();
}
