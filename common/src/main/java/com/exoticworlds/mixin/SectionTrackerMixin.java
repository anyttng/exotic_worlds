package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.LevelBindable;
import com.exoticworlds.engine.level.BoundLevelFold;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.server.level.SectionTracker;
import net.minecraft.server.level.ServerLevel;

@Mixin(SectionTracker.class)
public class SectionTrackerMixin implements LevelBindable {
    @Unique
    private final BoundLevelFold exotic_worlds$bound = new BoundLevelFold();

    @Override
    public void exotic_worlds$bindLevel(ServerLevel level) {
        this.exotic_worlds$bound.bind(level);
    }

    @WrapOperation(
            method = {"checkNeighborsAfterUpdate", "getComputedLevel"},
            at = @At(value = "INVOKE", target = InjectionTargets.SECTION_POS_OFFSET))
    private long exotic_worlds$physicalNeighborSection(
            long sectionNode, int stepX, int stepY, int stepZ, Operation<Long> original) {
        return this.exotic_worlds$bound.fold().foldSectionNode(original.call(sectionNode, stepX, stepY, stepZ));
    }
}
