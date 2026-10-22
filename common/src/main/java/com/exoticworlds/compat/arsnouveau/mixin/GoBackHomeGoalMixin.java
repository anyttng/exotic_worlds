package com.exoticworlds.compat.arsnouveau.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.accessors.LevelBindable;
import com.hollingsworth.arsnouveau.common.entity.goal.GoBackHomeGoal;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;

@Mixin(value = GoBackHomeGoal.class, remap = false)
public abstract class GoBackHomeGoalMixin {
    @Shadow
    Mob entity;

    @Inject(method = "<init>*", at = @At("RETURN"))
    private void exotic_worlds$bindRangeLevel(CallbackInfo ci) {
        if (this.entity.level() instanceof ServerLevel level) {
            ((LevelBindable) this).exotic_worlds$bindLevel(level);
        }
    }
}
