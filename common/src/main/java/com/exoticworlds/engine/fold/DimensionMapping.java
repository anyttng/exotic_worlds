package com.exoticworlds.engine.fold;

import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;

import net.minecraft.world.phys.Vec3;

public final class DimensionMapping {
    public static Vec3 map(WorldFold source, WorldFold destination, Vec3 position, double declaredScale) {
        TranslationLattice from = source.blockLattice();
        TranslationLattice to = destination.blockLattice();
        Vec3 mapped = destination.fold(new Vec3(
                position.x * to.x().scaleFrom(from.x(), declaredScale),
                position.y,
                position.z * to.z().scaleFrom(from.z(), declaredScale)));
        return mapped.x == position.x && mapped.z == position.z ? position : mapped;
    }

    private DimensionMapping() {
    }
}
