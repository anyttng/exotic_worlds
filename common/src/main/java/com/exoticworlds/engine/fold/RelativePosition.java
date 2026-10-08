package com.exoticworlds.engine.fold;

import java.util.function.UnaryOperator;

import net.minecraft.world.phys.Vec3;

public final class RelativePosition {
    public static Vec3 moved(Vec3 position, boolean relativeX, boolean relativeZ, Vec3 anchor,
            UnaryOperator<Vec3> move) {
        Vec3 held = new Vec3(relativeX ? anchor.x : position.x, position.y, relativeZ ? anchor.z : position.z);
        Vec3 moved = move.apply(held);
        if (moved.x == held.x && moved.z == held.z) {
            return position;
        }

        return new Vec3(
                relativeX ? position.x + (moved.x - held.x) : moved.x,
                position.y,
                relativeZ ? position.z + (moved.z - held.z) : moved.z);
    }

    private RelativePosition() {
    }
}
