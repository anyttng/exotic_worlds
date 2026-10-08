package com.exoticworlds.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.accessors.FramedStructureStart;
import com.exoticworlds.accessors.LevelHolder;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.NearestCopy;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

@Mixin(StructureManager.class)
public class StructureManagerMixin {
    @Shadow
    @Final
    private LevelAccessor level;

    @WrapMethod(
            method = "startsForStructure(IILjava/util/function/Predicate;)Ljava/util/List;")
    private List<StructureStart> toroidal$startsInTheAskingChunksFrame(int chunkX, int chunkZ, Predicate<Structure> matcher,
            Operation<List<StructureStart>> original) {
        List<StructureStart> starts = original.call(chunkX, chunkZ, matcher);
        if (!(this.level instanceof WorldGenRegion region)) {
            return starts;
        }

        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(((LevelHolder) region).toroidal$level());
        if (transformer != null && !starts.isEmpty()) {
            ChunkPos pos = new ChunkPos(chunkX, chunkZ);
            List<StructureStart> framed = new ArrayList<>(starts.size());
            for (StructureStart start : starts) {
                StructureStart inFrame = ((FramedStructureStart) (Object) start)
                        .toroidal$framedToward(region, transformer, pos);
                if (inFrame != null) {
                    framed.add(inFrame);
                }
            }

            starts = framed;
        }

        return starts;
    }

    @WrapOperation(
            method = {
                "getStructureAt(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/levelgen/structure/Structure;)Lnet/minecraft/world/level/levelgen/structure/StructureStart;",
                "getStructureAt(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/HolderSet;)Lnet/minecraft/world/level/levelgen/structure/StructureStart;"
            },
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/structure/BoundingBox;isInside(Lnet/minecraft/core/Vec3i;)Z"))
    private boolean toroidal$startBoxThroughSeam(BoundingBox box, Vec3i pos, Operation<Boolean> original,
            @Local(argsOnly = true) BlockPos blockPos) {
        return original.call(box, NearestCopy.toward(WorldLoopAttachments.transformerOfReader(this.level), box, blockPos));
    }

    @WrapMethod(method = "structureHasPieceAt(IIILnet/minecraft/world/level/levelgen/structure/StructureStart;)Z")
    private boolean toroidal$piecesThroughSeam(int x, int y, int z, StructureStart start, Operation<Boolean> original) {
        if (!start.isValid()) {
            return original.call(x, y, z, start);
        }

        BlockPos seated = NearestCopy.toward(WorldLoopAttachments.transformerOfReader(this.level),
                start.getBoundingBox(), new BlockPos(x, y, z));
        return original.call(seated.getX(), y, seated.getZ(), start);
    }
}
