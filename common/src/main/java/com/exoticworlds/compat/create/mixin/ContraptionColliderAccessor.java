package com.exoticworlds.compat.create.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.zurrtum.create.content.contraptions.AbstractContraptionEntity;

import net.minecraft.world.entity.Entity;

@Mixin(AbstractContraptionEntity.class)
public interface ContraptionColliderAccessor {
    @Accessor("collidingEntities")
    Map<Entity, ?> exotic_worlds$collidingEntities();
}
