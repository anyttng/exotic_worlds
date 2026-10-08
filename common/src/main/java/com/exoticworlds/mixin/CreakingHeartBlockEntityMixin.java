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
    private Vec3 toroidal$hurtTargetThroughSeam(Vec3 centre) {
        return toroidal$nearestToHeart((CreakingHeartBlockEntity) (Object) this, centre);
    }

    @ModifyExpressionValue(method = "lambda$serverTick$0", at = @At(value = "INVOKE", target = AABB_GET_CENTER))
    private static Vec3 toroidal$hurtRefreshThroughSeam(Vec3 centre,
            @Local(argsOnly = true) CreakingHeartBlockEntity heart) {
        return toroidal$nearestToHeart(heart, centre);
    }

    @Unique
    private static Vec3 toroidal$nearestToHeart(CreakingHeartBlockEntity heart, Vec3 centre) {
        BlockPos heartPos = heart.getBlockPos();
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(heart.getLevel());
        return NearestCopy.toward(transformer, Vec3.atCenterOf(heartPos), centre);
    }
}
