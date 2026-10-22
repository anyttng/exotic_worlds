package com.exoticworlds.compat.arsnouveau.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.accessors.LevelBindable;
import com.hollingsworth.arsnouveau.common.entity.Whirlisprig;
import com.hollingsworth.arsnouveau.common.entity.goal.whirlisprig.InspectPlantGoal;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

@Mixin(value = InspectPlantGoal.class, remap = false)
public abstract class InspectPlantGoalMixin {
    @Shadow
    Whirlisprig entity;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void exotic_worlds$bindRangeLevel(CallbackInfo ci) {
        if (((Entity) (Object) this.entity).level() instanceof ServerLevel level) {
            ((LevelBindable) this).exotic_worlds$bindLevel(level);
        }
    }
}
