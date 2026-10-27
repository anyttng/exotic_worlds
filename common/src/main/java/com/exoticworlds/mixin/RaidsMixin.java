package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.LevelBindable;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.NearestCopy;
import com.exoticworlds.engine.level.BoundLevelFold;
import com.exoticworlds.engine.seam.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.raid.Raids;

@Mixin(Raids.class)
public class RaidsMixin implements LevelBindable {
    @Unique
    private final BoundLevelFold exotic_worlds$bound = new BoundLevelFold();

    @Override
    public void exotic_worlds$bindLevel(ServerLevel level) {
        this.exotic_worlds$bound.bind(level);
    }

    @WrapOperation(
            method = "getNearbyRaid",
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_DIST_SQR))
    private double exotic_worlds$raidDistanceThroughSeam(BlockPos raidCenter, Vec3i pos, Operation<Double> original) {
        WorldFold fold = this.exotic_worlds$bound.wrapped();
        return fold == null ? original.call(raidCenter, pos) : SeamRange.sqr(fold, raidCenter, pos);
    }

    @WrapOperation(
            method = "createOrExtendRaid",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/village/poi/PoiRecord;getPos()Lnet/minecraft/core/BlockPos;"))
    private BlockPos exotic_worlds$poiInPlayerFrame(PoiRecord record, Operation<BlockPos> original,
            @Local(argsOnly = true) ServerPlayer player, @Local(argsOnly = true) BlockPos raidPosition) {
        BlockPos pos = original.call(record);
        WorldFold fold = WorldLoopAttachments.wrappedFoldOf(player.level());
        return NearestCopy.toward(fold, raidPosition, pos);
    }

    @WrapOperation(
            method = "createOrExtendRaid",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;containing(Lnet/minecraft/core/Position;)Lnet/minecraft/core/BlockPos;"))
    private BlockPos exotic_worlds$centerIntoBounds(Position mean, Operation<BlockPos> original,
            @Local(argsOnly = true) ServerPlayer player) {
        return WorldLoopAttachments.foldOf(player.level()).fold(original.call(mean));
    }
}
