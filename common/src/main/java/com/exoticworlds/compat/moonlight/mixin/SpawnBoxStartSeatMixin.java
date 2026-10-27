package com.exoticworlds.compat.moonlight.mixin;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.accessors.FramedStructureStart;
import com.exoticworlds.core.DeckTransformation;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.mixin.StructureManagerAccessor;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.mehvahdjukaar.moonlight.api.worldgen.ISpawnBoxStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

@Mixin(ISpawnBoxStructure.class)
public interface SpawnBoxStartSeatMixin {
    @WrapOperation(
            method = "ml$getSpecialSpawns",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/StructureManager;fillStartsForStructure(Lnet/minecraft/world/level/levelgen/structure/Structure;Lit/unimi/dsi/fastutil/longs/LongSet;Ljava/util/function/Consumer;)V"))
    private void exotic_worlds$startsFramedTowardSpawn(StructureManager manager, Structure structure,
            LongSet references, Consumer<StructureStart> consumer, Operation<Void> original,
            @Local(argsOnly = true) BlockPos pos) {
        LevelAccessor level = ((StructureManagerAccessor) manager).exotic_worlds$level();
        WorldFold fold = WorldLoopAttachments.foldOfReader(level);
        if (!fold.isWrapped() || !(level instanceof WorldGenLevel genLevel)) {
            original.call(manager, structure, references, consumer);
            return;
        }

        Consumer<StructureStart> framing = start -> {
            DeckTransformation move = fold.nearestCopyTransformation(pos, start.getBoundingBox().getCenter());
            if (move.isIdentity()) {
                consumer.accept(start);
                return;
            }

            StructureStart framed = ((FramedStructureStart) (Object) start).exotic_worlds$framedBy(genLevel, move);
            consumer.accept(framed != null ? framed : start);
        };
        original.call(manager, structure, references, framing);
    }
}
