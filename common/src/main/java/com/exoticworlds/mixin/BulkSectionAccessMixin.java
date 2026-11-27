package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import it.unimi.dsi.fastutil.longs.Long2ObjectFunction;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.BulkSectionAccess;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;

@Mixin(BulkSectionAccess.class)
public class BulkSectionAccessMixin {
    @Shadow
    @Final
    private LevelAccessor level;

    @WrapOperation(
            method = "getSection",
            at = @At(
                    value = "INVOKE",
                    target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;computeIfAbsent"
                            + "(JLit/unimi/dsi/fastutil/longs/Long2ObjectFunction;)Ljava/lang/Object;"))
    private Object exotic_worlds$keySectionByItsChunk(
            Long2ObjectMap<LevelChunkSection> sections,
            long sectionKey,
            Long2ObjectFunction<LevelChunkSection> acquire,
            Operation<Object> original,
            @Local(argsOnly = true) BlockPos pos,
            @Local int sectionIndex) {
        if (sections.containsKey(sectionKey)) {
            return original.call(sections, sectionKey, acquire);
        }

        ChunkAccess chunk = this.level.getChunk(
                SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
        ChunkPos own = chunk.getPos();
        long ownKey = SectionPos.asLong(own.x(), SectionPos.blockToSectionCoord(pos.getY()), own.z());
        Long2ObjectFunction<LevelChunkSection> acquireFromChunk = key -> {
            LevelChunkSection section = chunk.getSection(sectionIndex);
            section.acquire();
            return section;
        };
        return original.call(sections, ownKey, acquireFromChunk);
    }
}
