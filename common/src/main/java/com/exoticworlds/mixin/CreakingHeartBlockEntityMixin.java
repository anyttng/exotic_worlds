package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.NearestCopy;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.CreakingHeartBlockEntity;
import net.minecraft.world.phys.Vec3;

@Mixin(CreakingHeartBlockEntity.class)
public class CreakingHeartBlockEntityMixin {
    @Unique
    private static final String AABB_GET_CENTER =
            "Lnet/minecraft/world/phys/AABB;getCenter()Lnet/minecraft/world/phys/Vec3;";

    @ModifyExpressionValue(method = "creakingHurt", at = @At(value = "INVOKE", target = AABB_GET_CENTER))
    private Vec3 exotic_worlds$hurtTargetThroughSeam(Vec3 center) {
        return exotic_worlds$nearestToHeart((CreakingHeartBlockEntity) (Object) this, center);
    }

    @ModifyExpressionValue(method = "lambda$serverTick$0", at = @At(value = "INVOKE", target = AABB_GET_CENTER))
    private static Vec3 exotic_worlds$hurtRefreshThroughSeam(Vec3 center,
            @Local(argsOnly = true) CreakingHeartBlockEntity heart) {
        return exotic_worlds$nearestToHeart(heart, center);
    }

    @Unique
    private static Vec3 exotic_worlds$nearestToHeart(CreakingHeartBlockEntity heart, Vec3 center) {
        BlockPos heartPos = heart.getBlockPos();
        WorldFold fold = WorldLoopAttachments.wrappedFoldOf(heart.getLevel());
        return NearestCopy.toward(fold, Vec3.atCenterOf(heartPos), center);
    }
}
