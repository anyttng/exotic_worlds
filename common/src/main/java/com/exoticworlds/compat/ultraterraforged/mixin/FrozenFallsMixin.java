package com.exoticworlds.compat.ultraterraforged.mixin;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.compat.ultraterraforged.LapCarrier;
import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;

import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.rivermap.FrozenFalls;

import net.minecraft.world.level.chunk.ChunkAccess;

@Mixin(value = FrozenFalls.class, remap = false)
public abstract class FrozenFallsMixin {
    @WrapOperation(method = "apply", at = @At(value = "INVOKE",
            target = "Lcom/pandaismyname1/ultraterraforged/world/worldgen/rivermap/FrozenFalls;"
                    + "chunkAt(Lnet/minecraft/world/level/chunk/ChunkAccess;Ljava/util/List;II)"
                    + "Lnet/minecraft/world/level/chunk/ChunkAccess;"))
    private static @Nullable ChunkAccess exotic_worlds$foldSide(ChunkAccess center, List<ChunkAccess> region,
            int blockX, int blockZ, Operation<ChunkAccess> original,
            @Local(argsOnly = true) GeneratorContext context) {
        WorldFold fold = LapCarrier.foldOf(context.levels);
        if (fold == null) {
            return original.call(center, region, blockX, blockZ);
        }

        TranslationLattice lattice = fold.blockLattice();
        return original.call(center, region, lattice.foldX(blockX, blockZ), lattice.foldZ(blockZ));
    }
}
