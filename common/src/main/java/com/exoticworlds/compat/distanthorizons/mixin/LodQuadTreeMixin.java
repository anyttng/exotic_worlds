package com.exoticworlds.compat.distanthorizons.mixin;

import java.util.ArrayList;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.distanthorizons.DhClientShapes;
import com.exoticworlds.compat.distanthorizons.DhKeys;
import com.exoticworlds.compat.distanthorizons.DhLattice;
import com.exoticworlds.compat.distanthorizons.DhShapes;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.seibel.distanthorizons.core.level.IDhClientLevel;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos2D;
import com.seibel.distanthorizons.core.render.QuadTree.LodQuadTree;
import com.seibel.distanthorizons.core.render.QuadTree.LodRenderSection;
import com.seibel.distanthorizons.core.render.QuadTree.QuadTreeTickNodeHolder;
import com.seibel.distanthorizons.core.sql.dto.BeaconBeamDTO;
import com.seibel.distanthorizons.core.sql.repo.BeaconBeamRepo;
import com.seibel.distanthorizons.core.util.objects.quadTree.QuadTree;

@Mixin(LodQuadTree.class)
public class LodQuadTreeMixin {
    @Shadow
    @Final
    private IDhClientLevel level;

    @WrapMethod(method = "queuePosToReload")
    private void exotic_worlds$reloadTheNearestCopy(long pos, Operation<Void> original) {
        DhBlockPos2D center = exotic_worlds$center();
        original.call(DhKeys.nearestSection(DhShapes.of(this.level), center.x, center.z, pos));
    }

    @ModifyReturnValue(method = "calcExpectedDetailLevel*", at = @At("RETURN"))
    private byte exotic_worlds$capDetailAtTheWorld(byte expected) {
        return DhKeys.cappedDetail(DhShapes.of(this.level), expected);
    }

    @WrapMethod(method = "calcExpectedDetailLevel(Lcom/seibel/distanthorizons/core/pos/blockPos/DhBlockPos2D;J)B")
    private byte exotic_worlds$splitAStraddler(DhBlockPos2D playerPos, long sectionPos, Operation<Byte> original) {
        byte expected = original.call(playerPos, sectionPos);
        DhBlockPos2D center = exotic_worlds$center();
        return DhKeys.straddlerDetail(DhShapes.of(this.level), center.x, center.z, sectionPos, expected);
    }

    @WrapOperation(
            method = {"onDetailLevelTooLow", "onDesiredDetailLevel"},
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/seibel/distanthorizons/core/render/QuadTree/LodRenderSection;canRender()Z"))
    private boolean exotic_worlds$refuseAnIncompleteSection(LodRenderSection section, Operation<Boolean> original) {
        DhBlockPos2D center = exotic_worlds$center();
        return original.call(section)
                && DhKeys.drawsSection(DhShapes.of(this.level), center.x, center.z, section.pos);
    }

    @WrapOperation(
            method = "recursivelyUpdateRenderSectionNode",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/seibel/distanthorizons/core/render/QuadTree/QuadTreeTickNodeHolder;"
                            + "addLoadSection(Lcom/seibel/distanthorizons/core/render/QuadTree/LodRenderSection;)V"))
    private void exotic_worlds$loadOnlyTheNearestCopy(QuadTreeTickNodeHolder holder, LodRenderSection section,
            Operation<Void> original) {
        DhBlockPos2D center = exotic_worlds$center();
        if (DhKeys.isNearestCopy(DhShapes.of(this.level), center.x, center.z, section.pos)) {
            original.call(holder, section);
        }
    }

    @WrapOperation(
            method = "lambda$updateAllRenderSections$1",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/seibel/distanthorizons/core/pos/DhSectionPos;contains(JJ)Z"))
    private static boolean exotic_worlds$cancelByACopyOfTheSection(long sectionPos, long genPos,
            Operation<Boolean> original) {
        DhLattice lattice = DhClientShapes.current();
        return lattice == null
                ? original.call(sectionPos, genPos)
                : DhKeys.containsACopy(lattice, sectionPos, genPos);
    }

    @WrapOperation(
            method = "refreshRenderingBeacons",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/seibel/distanthorizons/core/sql/repo/BeaconBeamRepo;"
                            + "getAllBeamsInBlockPosRange(IIII)Ljava/util/ArrayList;"))
    private ArrayList<BeaconBeamDTO> exotic_worlds$drawOnlyTheNearestBeams(BeaconBeamRepo repo, int minBlockX,
            int maxBlockX, int minBlockZ, int maxBlockZ, Operation<ArrayList<BeaconBeamDTO>> original) {
        ArrayList<BeaconBeamDTO> beams = original.call(repo, minBlockX, maxBlockX, minBlockZ, maxBlockZ);
        DhBlockPos2D center = exotic_worlds$center();
        DhKeys.keepNearestBeams(DhShapes.of(this.level), center.x, center.z, beams);
        return beams;
    }

    @Unique
    private DhBlockPos2D exotic_worlds$center() {
        return ((QuadTree<?>) (Object) this).getCenterBlockPos();
    }
}
