package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.WorldBorder;

@Mixin(BorderChangeListener.DelegateBorderChangeListener.class)
public interface DelegateBorderChangeListenerAccessor {
    @Accessor("worldBorder")
    WorldBorder exotic_worlds$target();
}
