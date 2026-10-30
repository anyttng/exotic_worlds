package com.exoticworlds.compat.arsnouveau.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.seam.SeamRange;
import com.hollingsworth.arsnouveau.api.util.BlockUtil;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin(value = BlockUtil.class, remap = false)
public abstract class BlockUtilMixin {
    @WrapMethod(method = "distanceFrom(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)D")
    private static double exotic_worlds$blockDistanceThroughSeam(Level level, BlockPos start, BlockPos end,
            Operation<Double> original) {
        if (start == null || end == null) {
            return original.call(level, start, end);
        }

        return Math.sqrt(SeamRange.sqr(WorldLoopAttachments.wrappedFoldOfReader(level), start, end));
    }

    @WrapMethod(method = "distanceFrom(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/core/BlockPos;)D")
    private static double exotic_worlds$pointToBlockDistanceThroughSeam(Level level, Vec3 start, BlockPos end,
            Operation<Double> original) {
        if (start == null || end == null) {
            return original.call(level, start, end);
        }

        return Math.sqrt(SeamRange.sqr(WorldLoopAttachments.wrappedFoldOfReader(level), start,
                Vec3.atLowerCornerOf(end)));
    }

    @WrapMethod(method = "distanceFrom(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)D")
    private static double exotic_worlds$pointDistanceThroughSeam(Level level, Vec3 start, Vec3 end,
            Operation<Double> original) {
        return Math.sqrt(SeamRange.sqr(WorldLoopAttachments.wrappedFoldOfReader(level), start, end));
    }
}
