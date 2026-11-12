package com.exoticworlds.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.zurrtum.create.content.contraptions.gantry.GantryContraptionEntity;

import net.minecraft.core.Direction;

@Mixin(GantryContraptionEntity.class)
public interface GantryContraptionEntityAccessor {
    @Accessor("movementAxis")
    Direction exotic_worlds$movementAxis();
}
