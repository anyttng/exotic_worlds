package com.exoticworlds.accessors;

import org.jspecify.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;

public interface LevelBindable {
    default void exotic_worlds$bindLevel(ServerLevel level) {
    }

    default @Nullable ServerLevel exotic_worlds$boundLevel() {
        return null;
    }
}
