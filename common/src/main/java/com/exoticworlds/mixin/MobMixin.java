package com.exoticworlds.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.accessors.TransformerSource;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.fold.FoldedBoxQuery;
import com.exoticworlds.engine.fold.NearestCopy;
import com.exoticworlds.engine.seam.SeamAim;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

@Mixin(Mob.class)
public class MobMixin {
    @ModifyVariable(method = "lookAt(Lnet/minecraft/world/entity/Entity;FF)V", at = @At("STORE"), ordinal = 0)
    private double exotic_worlds$lookDeltaX(double deltaX, @Local(argsOnly = true) Entity target) {
        return SeamAim.deltaTo((Mob) (Object) this, target.position()).x;
    }

    @ModifyVariable(method = "lookAt(Lnet/minecraft/world/entity/Entity;FF)V", at = @At("STORE"), ordinal = 1)
    private double exotic_worlds$lookDeltaZ(double deltaZ, @Local(argsOnly = true) Entity target) {
        return SeamAim.deltaTo((Mob) (Object) this, target.position()).z;
    }

    @ModifyExpressionValue(
            method = "isWithinMeleeAttackRange",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getHitbox()Lnet/minecraft/world/phys/AABB;"))
    private AABB exotic_worlds$meleeHitboxThroughSeam(AABB hitbox) {
        Mob mob = (Mob) (Object) this;
        return FoldedBoxQuery.toward(mob, mob.position(), hitbox);
    }

    @ModifyExpressionValue(
            method = "isWithinRestriction(Lnet/minecraft/core/BlockPos;)Z",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/world/entity/Mob;restrictCenter:Lnet/minecraft/core/BlockPos;",
                    opcode = Opcodes.GETFIELD))
    private BlockPos exotic_worlds$homeThroughSeam(BlockPos home, @Local(argsOnly = true) BlockPos pos) {
        return exotic_worlds$nearestHome(home, pos);
    }

    @Unique
    private BlockPos exotic_worlds$nearestHome(BlockPos home, BlockPos anchor) {
        WorldFold transformer = ((TransformerSource) this).exotic_worlds$wrappedTransformer();
        return NearestCopy.toward(transformer, anchor, home);
    }
}
