package com.exoticworlds.compat.supplementaries.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.SeamHit;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

@Mixin(targets = "net.mehvahdjukaar.supplementaries.common.block.tiles.EndermanSkullBlockTile")
public class EndermanSkullWatchSeatMixin {
    @ModifyExpressionValue(method = "isBeingWatched", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;pick(DFZ)Lnet/minecraft/world/phys/HitResult;"))
    private static HitResult exotic_worlds$hitOnCanonicalSkull(HitResult hit, @Local(argsOnly = true) Level level) {
        return hit instanceof BlockHitResult block
                ? SeamHit.reseat(block, WorldLoopAttachments.transformerOf(level).foldOriented(block.getBlockPos()))
                : hit;
    }
}
