package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.gameevent.BlockPositionSource;

@Mixin(BlockPositionSource.class)
public interface BlockPositionSourceAccessor {
    @Accessor("pos")
    BlockPos toroidal$getPos();
}
