package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.exoticworlds.core.ShapedChunkGenerator;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

@Mixin(StructureCheck.class)
public class StructureCheckMixin {
    @Shadow
    @Final
    private ChunkGenerator chunkGenerator;

    @WrapMethod(method = "checkStart")
    private StructureCheckResult exotic_worlds$noStartPastTheBounds(ChunkPos pos, Structure structure,
            StructurePlacement placement, boolean requireUnreferenced, Operation<StructureCheckResult> original) {
        return ShapedChunkGenerator.foldOf(this.chunkGenerator).isOver(pos)
                ? StructureCheckResult.START_NOT_PRESENT
                : original.call(pos, structure, placement, requireUnreferenced);
    }

    @WrapMethod(method = "canCreateStructure")
    private boolean exotic_worlds$validateAgainstThisWorldsBiomes(ChunkPos pos, Structure structure,
            Operation<Boolean> original) {
        return GenerationFoldContext.withFold(
                ShapedChunkGenerator.foldOf(this.chunkGenerator), () -> original.call(pos, structure));
    }
}
