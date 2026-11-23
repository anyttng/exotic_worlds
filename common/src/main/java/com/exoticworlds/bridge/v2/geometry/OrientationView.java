package com.exoticworlds.bridge.v2.geometry;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import com.exoticworlds.api.v2.geometry.Orientation;
import com.exoticworlds.core.FoldOrientation;

import net.minecraft.world.phys.Vec3;

final class OrientationView implements Orientation {
    private static final Map<FoldOrientation, OrientationView> VIEWS = views();

    private final FoldOrientation orientation;

    private OrientationView(FoldOrientation orientation) {
        this.orientation = orientation;
    }

    static Orientation of(FoldOrientation orientation) {
        return Objects.requireNonNull(VIEWS.get(orientation), "every fold orientation has a view");
    }

    private static Map<FoldOrientation, OrientationView> views() {
        Map<FoldOrientation, OrientationView> views = new EnumMap<>(FoldOrientation.class);
        for (FoldOrientation orientation : FoldOrientation.values()) {
            views.put(orientation, new OrientationView(orientation));
        }
        return views;
    }

    @Override
    public boolean isIdentity() {
        return this.orientation.isIdentity();
    }

    @Override
    public boolean preservesHandedness() {
        return this.orientation.preservesHandedness();
    }

    @Override
    public Vec3 applyToDelta(Vec3 delta) {
        return this.orientation.applyToDelta(delta);
    }

    @Override
    public String toString() {
        return this.orientation.toString();
    }
}
