package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;

@Mixin(StructurePiece.class)
public interface StructurePieceAccessor {
    @Accessor("boundingBox")
    void exotic_worlds$setBoundingBox(BoundingBox box);
}
