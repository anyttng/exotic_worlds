package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;
import com.exoticworlds.engine.seam.SeamCommandErrors;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;

import net.minecraft.core.QuartPos;
import net.minecraft.server.commands.FillBiomeCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

@Mixin(FillBiomeCommand.class)
public class FillBiomeCommandMixin {
    @Inject(
            method = "fill(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Holder;Ljava/util/function/Predicate;Ljava/util/function/Consumer;)Lcom/mojang/datafixers/util/Either;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/structure/BoundingBox;getXSpan()I",
                    ordinal = 0),
            cancellable = true)
    private static void toroidal$refuseRegionAcrossSeam(
            CallbackInfoReturnable<Either<Integer, CommandSyntaxException>> cir,
            @Local(argsOnly = true) ServerLevel level,
            @Local BoundingBox region) {
        CommandSyntaxException refusal = SeamCommandErrors.refusalForAmbiguousRegion(
                WorldLoopAttachments.wrappedTransformerOf(level), region);
        if (refusal == null) {
            return;
        }

        cir.setReturnValue(Either.right(refusal));
    }

    @WrapOperation(
            method = "fill(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Holder;Ljava/util/function/Predicate;Ljava/util/function/Consumer;)Lcom/mojang/datafixers/util/Either;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/ChunkAccess;fillBiomesFromNoise(Lnet/minecraft/world/level/biome/BiomeResolver;Lnet/minecraft/world/level/biome/Climate$Sampler;)V"))
    private static void toroidal$fillInTheRegionsFrame(ChunkAccess chunk, BiomeResolver resolver,
            Climate.Sampler sampler, Operation<Void> original,
            @Local(argsOnly = true) ServerLevel level, @Local BoundingBox region) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        if (transformer == null) {
            original.call(chunk, resolver, sampler);
            return;
        }

        boolean foldX = toroidal$leavesTheWorld(transformer.bounds().x(), region.minX(), region.maxX());
        boolean foldZ = toroidal$leavesTheWorld(transformer.bounds().z(), region.minZ(), region.maxZ());
        if (!foldX && !foldZ) {
            original.call(chunk, resolver, sampler);
            return;
        }

        TranslationLattice lattice = transformer.blockLattice();
        int regionMinX = region.minX();
        int regionMinZ = region.minZ();
        BiomeResolver inFrame = (quartX, quartY, quartZ, resolverSampler) -> {
            int blockX = QuartPos.toBlock(quartX);
            int blockZ = QuartPos.toBlock(quartZ);
            int zLaps = foldZ ? Math.floorDiv(blockZ - regionMinZ, lattice.z().domainLength) : 0;
            int shiftedX = blockX - zLaps * lattice.skew();
            int frameX = foldX || shiftedX != blockX ? lattice.x().wrapFrom(regionMinX, shiftedX) : blockX;
            int frameZ = blockZ - zLaps * lattice.z().domainLength;
            return resolver.getNoiseBiome(
                    quartX + QuartPos.fromBlock(frameX - blockX), quartY, quartZ + QuartPos.fromBlock(frameZ - blockZ),
                    resolverSampler);
        };

        original.call(chunk, inFrame, sampler);
    }

    @Unique
    private static boolean toroidal$leavesTheWorld(AxisBounds axis, int minCoord, int maxCoord) {
        return axis.isOver(minCoord) || axis.isOver(maxCoord);
    }

}
