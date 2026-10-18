package com.exoticworlds.compat.supplementaries.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.FoldedBoxQuery;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Mixin(targets = "net.mehvahdjukaar.supplementaries.common.block.tiles.BellowsBlockTile")
public class BellowsGapSeatMixin {
    @ModifyArg(method = "pushEntities", index = 0, at = @At(value = "INVOKE",
            target = "Lnet/mehvahdjukaar/supplementaries/common/block/tiles/BellowsBlockTile;gapInFront(Lnet/minecraft/world/phys/AABB;Lnet/minecraft/core/Direction;)D"))
    private AABB toroidal$boxNearestBellows(AABB box, @Local(argsOnly = true) Level level) {
        Vec3 bellows = Vec3.atCenterOf(((BlockEntity) (Object) this).getBlockPos());
        return FoldedBoxQuery.toward(WorldLoopAttachments.transformerOf(level), bellows, box);
    }
}
