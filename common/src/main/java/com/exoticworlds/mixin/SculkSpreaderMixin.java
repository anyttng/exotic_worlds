package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.core.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.SculkSpreader;

@Mixin(SculkSpreader.class)
public class SculkSpreaderMixin {
    @WrapOperation(
            method = "updateCursors",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/SculkSpreader$ChargeCursor;update(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/level/block/SculkSpreader;Z)V"))
    private void toroidal$seatAfterUpdate(SculkSpreader.ChargeCursor cursor, LevelAccessor level, BlockPos originPos,
            RandomSource random, SculkSpreader spreader, boolean spreadVeins, Operation<Void> original) {
        original.call(cursor, level, originPos, random, spreader, spreadVeins);
        toroidal$seat(level, cursor, originPos);
    }

    @Unique
    private static void toroidal$seat(LevelAccessor level, SculkSpreader.ChargeCursor cursor, BlockPos originPos) {
        BlockPos raw = cursor.getPos();
        BlockPos seated = WorldLoopAttachments.transformerOfReader(level).nearestCopy(originPos, raw);
        if (seated != raw) {
            ((ChargeCursorAccessor) cursor).toroidal$setPos(seated);
        }
    }
}
