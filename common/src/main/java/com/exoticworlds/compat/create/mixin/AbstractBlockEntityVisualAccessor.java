package com.exoticworlds.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;

import net.minecraft.world.level.block.entity.BlockEntity;

// A @Shadow in GearboxVisualMixin cannot reach this Flywheel base-class field and leaves its handler silently absent.
@Mixin(value = AbstractBlockEntityVisual.class, remap = false)
public interface AbstractBlockEntityVisualAccessor {
    @Accessor("blockEntity")
    BlockEntity exotic_worlds$blockEntity();
}
