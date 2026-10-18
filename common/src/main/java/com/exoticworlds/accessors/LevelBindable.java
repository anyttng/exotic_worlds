package com.exoticworlds.accessors;

import net.minecraft.server.level.ServerLevel;

public interface LevelBindable {
    default void exotic_worlds$bindLevel(ServerLevel level) {
    }
}
