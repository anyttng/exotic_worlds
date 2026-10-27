package com.exoticworlds.engine.level;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;

import net.minecraft.world.phys.Vec3;

public final class CurrentRay {
    private static final ThreadLocal<@Nullable CurrentRay> CURRENT = new ThreadLocal<>();

    private final double minX;
    private final double maxX;
    private final double minZ;
    private final double maxZ;

    private @Nullable WorldFold decidedFor;
    private boolean meetsForeign;

    private CurrentRay(Vec3 from, Vec3 to) {
        this.minX = Math.min(from.x, to.x);
        this.maxX = Math.max(from.x, to.x);
        this.minZ = Math.min(from.z, to.z);
        this.maxZ = Math.max(from.z, to.z);
    }

    public static @Nullable CurrentRay enter(Vec3 from, Vec3 to) {
        CurrentRay outer = CURRENT.get();
        CURRENT.set(new CurrentRay(from, to));
        return outer;
    }

    public static void exit(@Nullable CurrentRay outer) {
        CURRENT.set(outer);
    }

    public static boolean meetsForeignFrame(WorldFold fold) {
        CurrentRay ray = CURRENT.get();
        return ray != null && ray.meetsForeign(fold);
    }

    private boolean meetsForeign(WorldFold fold) {
        if (fold != this.decidedFor) {
            TranslationLattice lattice = fold.blockLattice();
            this.meetsForeign = lattice.x().meetsForeign(this.minX, this.maxX)
                    || lattice.z().meetsForeign(this.minZ, this.maxZ);
            this.decidedFor = fold;
        }

        return this.meetsForeign;
    }
}
