package com.exoticworlds.compat.create.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.entity.Entity;

@Mixin(targets = "com.simibubi.create.content.contraptions.AbstractContraptionEntity", remap = false)
public interface ContraptionColliderAccessor {
    @Accessor("collidingEntities")
    Map<Entity, ?> toroidal$collidingEntities();
}
