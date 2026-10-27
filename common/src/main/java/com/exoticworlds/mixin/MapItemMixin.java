package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;

@Mixin(MapItem.class)
public class MapItemMixin {
    @WrapOperation(method = "update", at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_GET_X))
    private double exotic_worlds$holderNearestCenterX(Entity holder, Operation<Double> original,
            @Local(argsOnly = true) MapItemSavedData data) {
        double x = original.call(holder);
        WorldFold fold = WorldLoopAttachments.wrappedFoldOf(holder.level());
        return fold == null ? x : exotic_worlds$nearestHolder(fold, data, holder).x;
    }

    @WrapOperation(method = "update", at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_GET_Z))
    private double exotic_worlds$holderNearestCenterZ(Entity holder, Operation<Double> original,
            @Local(argsOnly = true) MapItemSavedData data) {
        double z = original.call(holder);
        WorldFold fold = WorldLoopAttachments.wrappedFoldOf(holder.level());
        return fold == null ? z : exotic_worlds$nearestHolder(fold, data, holder).z;
    }

    @WrapOperation(
            method = "update",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/saveddata/maps/MapItemSavedData;checkBanners(Lnet/minecraft/world/level/BlockGetter;II)V"))
    private void exotic_worlds$checkBannersInWorld(MapItemSavedData data, BlockGetter blockGetter, int x, int z,
            Operation<Void> original, @Local(argsOnly = true) Level level) {
        WorldFold fold = WorldLoopAttachments.wrappedFoldOf(level);
        if (fold == null) {
            original.call(data, blockGetter, x, z);
            return;
        }

        long folded = fold.foldBlockNode(BlockPos.asLong(x, 0, z));
        original.call(data, blockGetter, BlockPos.getX(folded), BlockPos.getZ(folded));
    }

    @Unique
    private static Vec3 exotic_worlds$nearestHolder(WorldFold fold, MapItemSavedData data, Entity holder) {
        return fold.nearestCopy(new Vec3(data.centerX, 0.0, data.centerZ), holder.position());
    }
}
