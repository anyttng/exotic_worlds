package com.exoticworlds.compat.mapfrontiers.mixin;

import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Mixin;

import com.exoticworlds.compat.mapfrontiers.ExoticWorldsGeometry;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import games.alejandrocoria.mapfrontiers.common.territory.frontier.GeometryQueries;
import games.alejandrocoria.mapfrontiers.platform.services.WorldGeometry;
import net.minecraft.core.BlockPos;

@Mixin(value = GeometryQueries.class, remap = false)
public class GeometryQueriesMixin {
    @WrapMethod(method = "anyCopyInBounds")
    private static boolean exotic_worlds$copiesOfTheShape(WorldGeometry geometry, BlockPos point, double minX,
            double maxX, double minZ, double maxZ, Predicate<BlockPos> test, Operation<Boolean> original) {
        return geometry instanceof ExoticWorldsGeometry ours
                ? ours.anyCopyInBounds(point, minX, maxX, minZ, maxZ, test)
                : original.call(geometry, point, minX, maxX, minZ, maxZ, test);
    }
}
