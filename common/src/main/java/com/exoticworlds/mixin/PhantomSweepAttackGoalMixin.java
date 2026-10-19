package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.fold.FoldedBoxQuery;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.phys.AABB;

@Mixin(targets = "net.minecraft.world.entity.monster.Phantom$PhantomSweepAttackGoal")
public class PhantomSweepAttackGoalMixin {
    // Two spellings of the same synthetic field: the NeoForge build reads Mojmap, where it stays this$0, while the
    // Fabric one runs on intermediary, which names every field — synthetic ones included — field_<n>.
    @Shadow(aliases = {"this$0", "field_7333"})
    @Final
    private Phantom phantom;

    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE", target = InjectionTargets.AABB_INTERSECTS))
    private boolean exotic_worlds$touchThroughSeam(AABB reach, AABB targetBox, Operation<Boolean> original) {
        AABB folded = FoldedBoxQuery.toward(this.phantom, this.phantom.position(), targetBox);
        return original.call(reach, folded);
    }
}
