package com.exoticworlds.compat.sable.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.exoticworlds.compat.sable.SeamFrame;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;

@Mixin(Biome.class)
public abstract class BiomeFrameMixin {
    @WrapMethod(method = "shouldFreeze(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Z)Z")
    private boolean exotic_worlds$frameOnFreeze(LevelReader reader, BlockPos pos, boolean mustBeAtEdge, Operation<Boolean> original) {
        return SeamFrame.with(reader, pos, Vec3::atCenterOf, original, reader, pos, mustBeAtEdge);
    }
}
