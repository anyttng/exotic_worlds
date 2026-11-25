package com.exoticworlds.compat.apprenticecodex.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.core.BlockPos;

@Mixin(targets = "jp.aquafactory.apprenticecodex.spell.linearbuild.LinearBuild$PlacementTarget", remap = false)
public interface LinearBuildPlacementTargetAccessor {
    @Accessor("hitPos")
    BlockPos exotic_worlds$getHitPos();
}
