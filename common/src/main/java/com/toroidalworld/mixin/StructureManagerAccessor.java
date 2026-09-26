package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.StructureCheck;

@Mixin(StructureManager.class)
public interface StructureManagerAccessor {
    @Accessor("structureCheck")
    StructureCheck toroidal$structureCheck();

    @Accessor("level")
    LevelAccessor toroidal$level();
}
