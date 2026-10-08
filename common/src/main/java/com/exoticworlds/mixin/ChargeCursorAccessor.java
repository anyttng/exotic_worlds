package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.SculkSpreader;

@Mixin(SculkSpreader.ChargeCursor.class)
public interface ChargeCursorAccessor {
    @Accessor("pos")
    void toroidal$setPos(BlockPos pos);
}
