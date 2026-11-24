package com.exoticworlds.engine.fold;

import com.exoticworlds.core.fold.TranslationLattice;
import com.exoticworlds.core.fold.WorldFold;

import net.minecraft.world.phys.Vec3;

public final class DimensionMapping {
    public static Vec3 map(WorldFold source, WorldFold destination, Vec3 position, double declaredScale) {
        TranslationLattice from = source.getBlockLattice();
        TranslationLattice to = destination.getBlockLattice();
        Vec3 mapped = destination.fold(new Vec3(
                position.x * to.getX().scaleFrom(from.getX(), declaredScale),
                position.y,
                position.z * to.getZ().scaleFrom(from.getZ(), declaredScale)));
        return mapped.x == position.x && mapped.z == position.z ? position : mapped;
    }

    private DimensionMapping() {
    }
}
