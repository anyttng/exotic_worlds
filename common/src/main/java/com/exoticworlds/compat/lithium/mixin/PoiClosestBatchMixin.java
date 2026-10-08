package com.exoticworlds.compat.lithium.mixin;

import java.util.Collection;
import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.datafixers.util.Pair;
import com.exoticworlds.compat.lithium.LithiumInjectionTargets;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.mixin.SectionStorageAccessor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;

@Mixin(value = PoiManager.class, priority = 1100)
public class PoiClosestBatchMixin {
    @WrapMethod(method = LithiumInjectionTargets.CLOSEST_BATCH)
    private Collection<Pair<Holder<PoiType>, BlockPos>> toroidal$batchThroughSeam(
            Predicate<Holder<PoiType>> typeFilter,
            Predicate<BlockPos> posFilter,
            BlockPos center,
            int radius,
            PoiManager.Occupancy status,
            long count,
            Operation<Collection<Pair<Holder<PoiType>, BlockPos>>> original) {
        if (!(((SectionStorageAccessor) this).toroidal$getLevelHeightAccessor() instanceof ServerLevel level)
                || WorldLoopAttachments.wrappedTransformerOf(level) == null) {
            return original.call(typeFilter, posFilter, center, radius, status, count);
        }

        return ((PoiManager) (Object) this).findAllClosestFirstWithType(typeFilter, posFilter, center, radius, status)
                .limit(count)
                .toList();
    }
}
