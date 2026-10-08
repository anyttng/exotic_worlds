package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheck;

@Mixin(StructureCheck.class)
public interface StructureCheckAccessor {
    @Invoker("canCreateStructure")
    boolean toroidal$canCreateStructure(ChunkPos pos, Structure structure);
}
