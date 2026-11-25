package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import io.redspace.ironsspellbooks.entity.mobs.goals.WizardAttackGoal;

import net.minecraft.world.entity.PathfinderMob;

@Mixin(value = WizardAttackGoal.class, remap = false)
public interface WizardAttackGoalAccessor {
    @Accessor("mob")
    PathfinderMob exotic_worlds$getMob();
}
