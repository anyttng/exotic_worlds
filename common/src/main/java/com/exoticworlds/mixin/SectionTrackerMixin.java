package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.LevelBindable;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.level.BoundLevelFold;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.server.level.SectionTracker;
import net.minecraft.server.level.ServerLevel;

@Mixin(SectionTracker.class)
public class SectionTrackerMixin implements LevelBindable {
    @Unique
    private final BoundLevelFold toroidal$bound = new BoundLevelFold();

    @Override
    public void toroidal$bindLevel(ServerLevel level) {
        this.toroidal$bound.bind(level);
    }

    @WrapOperation(
            method = {"checkNeighborsAfterUpdate", "getComputedLevel"},
            at = @At(value = "INVOKE", target = InjectionTargets.SECTION_POS_OFFSET))
    private long toroidal$physicalNeighborSection(
            long sectionNode, int stepX, int stepY, int stepZ, Operation<Long> original) {
        long neighbor = original.call(sectionNode, stepX, stepY, stepZ);
        WorldFold transformer = this.toroidal$bound.wrapped();
        return transformer == null ? neighbor : transformer.foldSectionNode(neighbor);
    }
}
