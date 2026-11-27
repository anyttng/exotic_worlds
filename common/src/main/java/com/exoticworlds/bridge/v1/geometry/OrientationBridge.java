package com.exoticworlds.bridge.v1.geometry;

import com.exoticworlds.core.fold.FoldOrientation;

import net.minecraft.world.phys.Vec3;

public final class OrientationBridge {

    public static boolean preservesHandedness(boolean flipsX, boolean flipsZ) {
        return FoldOrientation.of(false, flipsX, flipsZ).isHandednessPreserved();
    }

    public static Vec3 applyToDelta(boolean flipsX, boolean flipsZ, Vec3 delta) {
        return FoldOrientation.of(false, flipsX, flipsZ).applyToDelta(delta);
    }

    private OrientationBridge() {
    }
}
