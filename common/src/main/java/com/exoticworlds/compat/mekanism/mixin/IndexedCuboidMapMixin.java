package com.exoticworlds.compat.mekanism.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.mekanism.RadiationBoxFrame;
import com.exoticworlds.compat.mekanism.RadiationIndexFrame;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;

import mekanism.common.lib.collection.BiLongMultimap;
import mekanism.common.lib.collection.IndexedCuboidMap;
import mekanism.common.util.ChunkUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

@Mixin(value = IndexedCuboidMap.class, remap = false)
public class IndexedCuboidMapMixin implements RadiationIndexFrame {
    private static final String TRACK_BOX = "track(Ljava/lang/Object;Lnet/minecraft/core/BlockPos;IIIIII)V";

    @Shadow
    @Final
    private BiLongMultimap<Object> chunkIndex;

    @Shadow
    @Final
    private Map<Object, Object> valueMap;

    @Unique
    private WorldFold toroidal$fold = WorldFolds.NOOP;

    @Override
    public WorldFold toroidal$fold() {
        return this.toroidal$fold;
    }

    @Override
    public void toroidal$bind(WorldFold fold) {
        if (fold == this.toroidal$fold) {
            return;
        }

        this.toroidal$fold = fold;
        this.chunkIndex.clear();
        for (Object box : this.valueMap.keySet()) {
            RadiationBoxFrame frame = (RadiationBoxFrame) box;
            frame.toroidal$setFold(fold);
            long[] keys = ChunkUtils.rangeClosed(
                    SectionPos.blockToSectionCoord(frame.toroidal$minX()),
                    SectionPos.blockToSectionCoord(frame.toroidal$minZ()),
                    SectionPos.blockToSectionCoord(frame.toroidal$maxX()),
                    SectionPos.blockToSectionCoord(frame.toroidal$maxZ()));
            for (long key : keys) {
                this.chunkIndex.put(fold.foldChunkKey(key), box);
            }
        }
    }

    @WrapOperation(method = TRACK_BOX,
            at = @At(value = "INVOKE", target = InjectionTargets.MAP_PUT))
    private Object toroidal$bindBoxFold(Map<Object, Object> values, Object box, Object value,
            Operation<Object> original) {
        ((RadiationBoxFrame) box).toroidal$setFold(this.toroidal$fold);
        return original.call(values, box, value);
    }

    @WrapOperation(method = TRACK_BOX,
            at = @At(value = "INVOKE",
                    target = "Lmekanism/common/lib/collection/BiLongMultimap;put(JLjava/lang/Object;)Z"))
    private boolean toroidal$fileUnderFoldedChunk(BiLongMultimap<Object> index, long key, Object box,
            Operation<Boolean> original) {
        return original.call(index, this.toroidal$fold.foldChunkKey(key), box);
    }

    @WrapOperation(method = TRACK_BOX,
            at = @At(value = "INVOKE",
                    target = "Lmekanism/common/lib/collection/BiLongMultimap;putAll([JLjava/lang/Object;)Z"))
    private boolean toroidal$fileUnderFoldedChunks(BiLongMultimap<Object> index, long[] keys, Object box,
            Operation<Boolean> original) {
        long[] folded = new long[keys.length];
        for (int i = 0; i < keys.length; i++) {
            folded[i] = this.toroidal$fold.foldChunkKey(keys[i]);
        }

        return original.call(index, folded, box);
    }

    @WrapOperation(method = {"find", "findFirstAt"},
            at = @At(value = "INVOKE", target = InjectionTargets.CHUNK_POS_AS_LONG_BLOCK),
            require = 2,
            expect = 2)
    private long toroidal$lookUpFoldedChunk(BlockPos pos, Operation<Long> original) {
        return this.toroidal$fold.foldChunkKey(original.call(pos));
    }

    @ModifyArg(method = "allCenteredInChunk(J)Ljava/util/Iterator;",
            at = @At(value = "INVOKE",
                    target = "Lmekanism/common/lib/collection/BiLongMultimap;getValues(J)Ljava/util/Set;"))
    private long toroidal$readFoldedChunk(long chunkKey) {
        return this.toroidal$fold.foldChunkKey(chunkKey);
    }
}
