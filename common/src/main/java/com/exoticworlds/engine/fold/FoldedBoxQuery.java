package com.exoticworlds.engine.fold;

import com.exoticworlds.accessors.FoldSource;
import com.exoticworlds.core.WorldFold;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class FoldedBoxQuery {
    public static AABB toward(@Nullable WorldFold fold, Vec3 anchor, AABB box) {
        return fold == null ? box : fold.nearestCopyOriented(anchor, box).value();
    }

    public static AABB toward(Entity levelSource, Vec3 anchor, AABB box) {
        return toward(((FoldSource) levelSource).exotic_worlds$wrappedFold(), anchor, box);
    }

    private FoldedBoxQuery() {
    }
}
