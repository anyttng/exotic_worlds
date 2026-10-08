package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.TransformerSource;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.fold.FoldedBoxQuery;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.phys.AABB;

@Mixin(targets = "net.minecraft.world.entity.monster.Vex$VexChargeAttackGoal")
public class VexChargeAttackGoalMixin {
    @Shadow(aliases = {"this$0", "field_7412"})
    @Final
    private Vex vex;

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = InjectionTargets.AABB_INTERSECTS))
    private boolean toroidal$touchThroughSeam(AABB vexBox, AABB targetBox, Operation<Boolean> original) {
        WorldFold transformer = ((TransformerSource) this.vex).toroidal$wrappedTransformer();
        AABB folded = FoldedBoxQuery.toward(transformer, this.vex.position(), targetBox);
        return original.call(vexBox, folded);
    }
}
