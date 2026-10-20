package com.exoticworlds.accessors;

import net.minecraft.world.phys.Vec3;

public interface MoveCheckpoints {
    Vec3 exotic_worlds$firstGood(double y);

    Vec3 exotic_worlds$lastGood(double y);

    void exotic_worlds$setGood(Vec3 first, Vec3 last);

    void exotic_worlds$setVehicleGood(Vec3 first, Vec3 last);
}
