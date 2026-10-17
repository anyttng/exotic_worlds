package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.exoticworlds.core.ShapedChunkGenerator;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
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
    private StructureCheckResult toroidal$noStartPastTheBounds(ChunkPos pos, Structure structure,
            StructurePlacement placement, boolean requireUnreferenced, Operation<StructureCheckResult> original) {
        return ShapedChunkGenerator.transformerOf(this.chunkGenerator).isOver(pos)
                ? StructureCheckResult.START_NOT_PRESENT
                : original.call(pos, structure, placement, requireUnreferenced);
    }

    @WrapMethod(method = "canCreateStructure")
    private boolean toroidal$validateAgainstThisWorldsBiomes(ChunkPos pos, Structure structure,
            Operation<Boolean> original) {
        return GenerationTransformerContext.withTransformer(
                ShapedChunkGenerator.transformerOf(this.chunkGenerator), () -> original.call(pos, structure));
    }
}
