package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.NearestCopy;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.PotentSulfurBlockEntity;
import net.minecraft.world.phys.Vec3;

@Mixin(PotentSulfurBlockEntity.class)
public class PotentSulfurBlockEntityMixin {
    @WrapMethod(method = "canBeReachedByNoxiousGas")
    private static boolean exotic_worlds$gasReachThroughSeam(Level level, BlockPos sourceBlock, Vec3 pos,
            Operation<Boolean> original) {
        WorldFold fold = WorldLoopAttachments.wrappedFoldOf(level);
        return original.call(level, sourceBlock, NearestCopy.toward(fold, Vec3.atCenterOf(sourceBlock), pos));
    }
}
