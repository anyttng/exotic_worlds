package com.exoticworlds.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.zurrtum.create.client.flywheel.lib.visual.AbstractBlockEntityVisual;

import net.minecraft.world.level.block.entity.BlockEntity;

@Mixin(AbstractBlockEntityVisual.class)
public interface AbstractBlockEntityVisualAccessor {
    @Accessor("blockEntity")
    BlockEntity exotic_worlds$blockEntity();
}
