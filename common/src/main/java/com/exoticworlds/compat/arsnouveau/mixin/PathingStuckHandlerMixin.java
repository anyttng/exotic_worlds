package com.exoticworlds.compat.arsnouveau.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.exoticworlds.accessors.NavigationShifter;
import com.hollingsworth.arsnouveau.common.entity.pathfinding.PathingStuckHandler;

import net.minecraft.core.BlockPos;

@Mixin(value = PathingStuckHandler.class, remap = false)
public abstract class PathingStuckHandlerMixin implements NavigationShifter {
    @Shadow
    private BlockPos prevDestination;

    @Shadow
    private BlockPos moveAwayStartPos;

    @Override
    public void exotic_worlds$shiftBy(int shiftX, int shiftZ) {
        if (!BlockPos.ZERO.equals(this.prevDestination)) {
            this.prevDestination = this.prevDestination.offset(shiftX, 0, shiftZ);
        }

        if (!BlockPos.ZERO.equals(this.moveAwayStartPos)) {
            this.moveAwayStartPos = this.moveAwayStartPos.offset(shiftX, 0, shiftZ);
        }
    }
}
