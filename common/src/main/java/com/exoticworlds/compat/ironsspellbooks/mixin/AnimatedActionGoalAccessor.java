package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss.goals.AnimatedActionGoal;

import net.minecraft.world.entity.Mob;

@Mixin(value = AnimatedActionGoal.class, remap = false)
public interface AnimatedActionGoalAccessor {
    @Accessor("mob")
    Mob exotic_worlds$getMob();
}
