package com.exoticworlds.compat.simpleclouds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import dev.nonamecrackers2.simpleclouds.common.world.CloudManager;

import net.minecraft.world.level.Level;

@Mixin(value = CloudManager.class, remap = false)
public interface CloudManagerAccessor {
    @Accessor("level")
    Level toroidal$level();
}
