package com.exoticworlds.compat.journeymap.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.exoticworlds.compat.journeymap.JourneyMapFold;

import net.minecraft.core.BlockPos;

@Mixin(targets = "journeymap.client.ui.option.LocationFormat$LocationFormatKeys", remap = false)
public class LocationFormatMixin {
    @WrapMethod(method = "format")
    private String toroidal$foldLocation(boolean verbose, int x, int z, int y, int vslice,
            Operation<String> original) {
        BlockPos folded = JourneyMapFold.foldUiBlock(new BlockPos(x, y, z));
        return original.call(verbose, folded.getX(), folded.getZ(), y, vslice);
    }
}
