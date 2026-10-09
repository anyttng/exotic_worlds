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
    private boolean toroidal$holdOneLapAroundTheCentre(long pos, Operation<Boolean> original) {
        boolean inSquare = original.call(pos);
        if (!inSquare || !((Object) this instanceof LodQuadTree tree)) {
            return inSquare;
        }

        DhLattice lattice = DhShapes.of(((LodQuadTreeAccessor) tree).toroidal$level());
        if (lattice == null) {
            return true;
        }

        DhBlockPos2D center = tree.getCenterBlockPos();
        int width = DhSectionPos.getBlockWidth(pos);
        return DhFold.overlapsNearestLap(lattice, Direction.Axis.X, center.x, DhSectionPos.getX(pos) * width, width)
                && DhFold.overlapsNearestLap(lattice, Direction.Axis.Z, center.z, DhSectionPos.getZ(pos) * width, width);
    }
}
