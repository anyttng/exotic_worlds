package com.exoticworlds.mixin;

import java.util.Comparator;
import java.util.Optional;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.FoldedOrder;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.portal.PortalForcer;

@Mixin(value = PortalForcer.class, priority = 1100)
public class PortalForcerMixin {
    private static final int NETHER_SEARCH_RADIUS = 16;
    private static final int OVERWORLD_SEARCH_RADIUS = 128;

    @Shadow
    @Final
    protected ServerLevel level;

    @WrapMethod(method = "findClosestPortalPosition")
    private Optional<BlockPos> toroidal$nearestThroughSeam(
            BlockPos approximateExitPos,
            boolean toNether,
            WorldBorder worldBorder,
            Operation<Optional<BlockPos>> original) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(this.level);
        if (transformer == null) {
            return original.call(approximateExitPos, toNether, worldBorder);
        }

        PoiManager poiManager = this.level.getPoiManager();
        int radius = toNether ? NETHER_SEARCH_RADIUS : OVERWORLD_SEARCH_RADIUS;
        poiManager.ensureLoadedAndValid(this.level, approximateExitPos, radius);
        Comparator<BlockPos> byDistance = Comparator.<BlockPos>comparingDouble(pos -> pos.distSqr(approximateExitPos))
                .thenComparingInt(Vec3i::getY);
        return poiManager.getInSquare(type -> type.is(PoiTypes.NETHER_PORTAL), approximateExitPos, radius,
                        PoiManager.Occupancy.ANY)
                .map(PoiRecord::getPos)
                .filter(worldBorder::isWithinBounds)
                .filter(pos -> this.level.getBlockState(pos).hasProperty(BlockStateProperties.HORIZONTAL_AXIS))
                .min(FoldedOrder.around(byDistance, transformer, approximateExitPos));
    }
}
