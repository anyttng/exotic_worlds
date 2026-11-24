package com.exoticworlds.compat.distanthorizons.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.exoticworlds.compat.distanthorizons.DhFold;
import com.exoticworlds.compat.distanthorizons.DhLattice;
import com.exoticworlds.compat.distanthorizons.DhShapes;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos2D;
import com.seibel.distanthorizons.core.render.QuadTree.LodQuadTree;
import com.seibel.distanthorizons.core.util.objects.quadTree.QuadTree;

import net.minecraft.core.Direction;

@Mixin(QuadTree.class)
public class QuadTreeMixin {
    @WrapMethod(method = "isSectionPosInBounds(J)Z")
    private boolean exotic_worlds$holdOneLapAroundTheCenter(long pos, Operation<Boolean> original) {
        boolean isInSquare = original.call(pos);
        if (!isInSquare || !((Object) this instanceof LodQuadTree tree)) {
            return isInSquare;
        }

        DhLattice lattice = DhShapes.of(((LodQuadTreeAccessor) tree).exotic_worlds$getLevel());
        if (lattice == null) {
            return true;
        }

        DhBlockPos2D center = tree.getCenterBlockPos();
        int widthBlocks = DhSectionPos.getBlockWidth(pos);
        return DhFold.isOverlappingNearestLap(lattice, Direction.Axis.X, center.x, DhSectionPos.getX(pos) * widthBlocks, widthBlocks)
                && DhFold.isOverlappingNearestLap(lattice, Direction.Axis.Z, center.z, DhSectionPos.getZ(pos) * widthBlocks, widthBlocks);
    }
}
