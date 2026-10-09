package com.exoticworlds.compat.distanthorizons.mixin;

import java.util.ArrayList;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.distanthorizons.DhFold;
import com.exoticworlds.compat.distanthorizons.DhKeys;
import com.exoticworlds.compat.distanthorizons.DhLattice;
import com.exoticworlds.compat.distanthorizons.DhShapes;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.seibel.distanthorizons.core.level.IDhClientLevel;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
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
    private void toroidal$reloadTheNearestCopy(long pos, Operation<Void> original) {
        DhLattice lattice = DhShapes.of(this.level);
        if (lattice == null) {
            original.call(pos);
            return;
        }

        DhBlockPos2D center = ((QuadTree<?>) (Object) this).getCenterBlockPos();
        original.call(DhKeys.nearestSection(lattice, center.x, center.z, pos));
    }

    @ModifyReturnValue(method = "calcExpectedDetailLevel*", at = @At("RETURN"))
    private byte toroidal$capDetailAtTheWorld(byte expected) {
        DhLattice lattice = DhShapes.of(this.level);
        if (lattice == null) {
            return expected;
        }

        byte cap = DhFold.maxExpectedDetailLevel(lattice, DhSectionPos.SECTION_MINIMUM_DETAIL_LEVEL);
        return expected <= cap ? expected : cap;
    }

    @WrapMethod(method = "calcExpectedDetailLevel(Lcom/seibel/distanthorizons/core/pos/blockPos/DhBlockPos2D;J)B")
    private byte toroidal$splitAStraddler(DhBlockPos2D playerPos, long sectionPos, Operation<Byte> original) {
        byte expected = original.call(playerPos, sectionPos);
        DhLattice lattice = DhShapes.of(this.level);
        if (lattice == null) {
            return expected;
        }

        DhBlockPos2D center = ((QuadTree<?>) (Object) this).getCenterBlockPos();
        byte leaf = DhSectionPos.SECTION_MINIMUM_DETAIL_LEVEL;
        if (DhKeys.straddlesNearestCopy(lattice, center.x, center.z, sectionPos)) {
            byte cap = (byte) (DhKeys.snapLevel(lattice) - leaf);
            return expected <= cap ? expected : cap;
        }

        if (DhKeys.straddlesNearestCopy(lattice, center.x, center.z, DhSectionPos.getParentPos(sectionPos))) {
            byte cap = (byte) (DhSectionPos.getDetailLevel(sectionPos) + 1 - leaf);
            return expected <= cap ? expected : cap;
        }

        return expected;
    }

    @WrapOperation(
            method = {"onDetailLevelTooLow", "onDesiredDetailLevel"},
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/seibel/distanthorizons/core/render/QuadTree/LodRenderSection;canRender()Z"))
    private boolean toroidal$refuseAnIncompleteSection(LodRenderSection section, Operation<Boolean> original) {
        boolean canRender = original.call(section);
        DhLattice lattice = DhShapes.of(this.level);
        if (lattice == null) {
            return canRender;
        }

        byte detailLevel = DhSectionPos.getDetailLevel(section.pos);
        int sectionX = DhSectionPos.getX(section.pos);
        int sectionZ = DhSectionPos.getZ(section.pos);
        DhBlockPos2D center = ((QuadTree<?>) (Object) this).getCenterBlockPos();
        return canRender
                && DhFold.isCompleteSection(lattice, DhKeys.LEAF, detailLevel, sectionX, sectionZ)
                && DhKeys.isNearestCopy(lattice, center.x, center.z, section.pos);
    }

    @WrapOperation(
            method = "recursivelyUpdateRenderSectionNode",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/seibel/distanthorizons/core/render/QuadTree/QuadTreeTickNodeHolder;"
                            + "addLoadSection(Lcom/seibel/distanthorizons/core/render/QuadTree/LodRenderSection;)V"))
    private void toroidal$loadOnlyTheNearestCopy(QuadTreeTickNodeHolder holder, LodRenderSection section,
            Operation<Void> original) {
        DhLattice lattice = DhShapes.of(this.level);
        if (lattice == null) {
            original.call(holder, section);
            return;
        }

        DhBlockPos2D center = ((QuadTree<?>) (Object) this).getCenterBlockPos();
        if (DhKeys.isNearestCopy(lattice, center.x, center.z, section.pos)) {
            original.call(holder, section);
        }
    }

    @WrapOperation(
            method = "lambda$updateAllRenderSections$1",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/seibel/distanthorizons/core/pos/DhSectionPos;contains(JJ)Z"))
    private static boolean toroidal$cancelByACopyOfTheSection(long sectionPos, long genPos,
            Operation<Boolean> original) {
        DhLattice lattice = DhShapes.current();
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
    private ArrayList<BeaconBeamDTO> toroidal$drawOnlyTheNearestBeams(BeaconBeamRepo repo, int minBlockX,
            int maxBlockX, int minBlockZ, int maxBlockZ, Operation<ArrayList<BeaconBeamDTO>> original) {
        ArrayList<BeaconBeamDTO> beams = original.call(repo, minBlockX, maxBlockX, minBlockZ, maxBlockZ);
        DhLattice lattice = DhShapes.of(this.level);
        if (lattice == null || beams == null) {
            return beams;
        }

        DhBlockPos2D center = ((QuadTree<?>) (Object) this).getCenterBlockPos();
        beams.removeIf(beam -> !DhKeys.isNearestBeam(lattice, center.x, center.z, beam.blockPos));
        return beams;
    }
}
