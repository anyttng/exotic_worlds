package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.SeamDelta;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;

@Mixin(TargetGoal.class)
public class TargetGoalMixin {
    @Shadow
    @Final
    protected Mob mob;

    @ModifyVariable(method = "canReach", at = @At(value = "STORE", ordinal = 0), ordinal = 0)
    private int toroidal$reachDeltaX(int deltaX) {
        return SeamDelta.foldX(toroidal$transformer(), deltaX);
    }

    @ModifyVariable(method = "canReach", at = @At(value = "STORE", ordinal = 0), ordinal = 1)
    private int toroidal$reachDeltaZ(int deltaZ) {
        return SeamDelta.foldZ(toroidal$transformer(), deltaZ);
    }

    @Unique
    private WorldFold toroidal$transformer() {
        return WorldLoopAttachments.transformerOf(this.mob.level());
    }
}
