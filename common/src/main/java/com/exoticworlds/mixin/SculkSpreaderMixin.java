package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.core.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.SculkSpreader;

@Mixin(SculkSpreader.class)
public class SculkSpreaderMixin {
    @WrapOperation(
            method = "updateCursors",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/SculkSpreader$ChargeCursor;isPosUnreasonable(Lnet/minecraft/core/BlockPos;)Z"))
    private boolean exotic_worlds$seatBeforeRangeGate(SculkSpreader.ChargeCursor cursor, BlockPos originPos,
            Operation<Boolean> original, @Local(argsOnly = true) LevelAccessor level) {
        exotic_worlds$seat(level, cursor, originPos);
        return original.call(cursor, originPos);
    }

    @WrapOperation(
            method = "updateCursors",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/SculkSpreader$ChargeCursor;update(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/level/block/SculkSpreader;Z)V"))
    private void exotic_worlds$seatAfterUpdate(SculkSpreader.ChargeCursor cursor, LevelAccessor level, BlockPos originPos,
            RandomSource random, SculkSpreader spreader, boolean spreadVeins, Operation<Void> original) {
        original.call(cursor, level, originPos, random, spreader, spreadVeins);
        exotic_worlds$seat(level, cursor, originPos);
    }

    @Unique
    private static void exotic_worlds$seat(LevelAccessor level, SculkSpreader.ChargeCursor cursor, BlockPos originPos) {
        BlockPos raw = cursor.getPos();
        BlockPos seated = WorldLoopAttachments.foldOfReader(level).nearestCopy(originPos, raw);
        if (seated != raw) {
            ((ChargeCursorAccessor) cursor).exotic_worlds$setPos(seated);
        }
    }
}
