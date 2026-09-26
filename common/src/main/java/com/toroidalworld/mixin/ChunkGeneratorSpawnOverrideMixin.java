package com.toroidalworld.mixin;

import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldLoopAttachments;
import com.toroidalworld.engine.fold.NearestCopy;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import net.minecraft.world.level.levelgen.structure.StructureStart;

@Mixin(ChunkGenerator.class)
public class ChunkGeneratorSpawnOverrideMixin {
    @ModifyVariable(method = "getMobsAt", at = @At("STORE"))
    private Predicate<StructureStart> toroidal$fullBoxThroughSeam(Predicate<StructureStart> check,
            @Local StructureSpawnOverride override, @Local(argsOnly = true) Level level,
            @Local(argsOnly = true) BlockPos pos) {
        if (override.boundingBox() == StructureSpawnOverride.BoundingBoxType.PIECE) {
            return check;
        }

        WorldFold fold = WorldLoopAttachments.transformerOf(level);
        return start -> start.getBoundingBox().isInside(NearestCopy.toward(fold, start.getBoundingBox(), pos));
    }
}
