package com.exoticworlds.compat.betterend.mixin;

import java.awt.Point;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.betterx.betterend.world.generator.IslandLayer;
import org.betterx.betterend.world.generator.TerrainBoolCache;
import org.betterx.betterend.world.generator.TerrainGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(TerrainGenerator.class)
public interface TerrainGeneratorAccessor {
    @Accessor("LOCKER")
    static ReentrantLock toroidal$locker() {
        throw new AssertionError();
    }

    @Accessor("largeIslands")
    static IslandLayer toroidal$largeIslands() {
        throw new AssertionError();
    }

    @Accessor("mediumIslands")
    static IslandLayer toroidal$mediumIslands() {
        throw new AssertionError();
    }

    @Accessor("smallIslands")
    static IslandLayer toroidal$smallIslands() {
        throw new AssertionError();
    }

    @Accessor("TERRAIN_BOOL_CACHE_MAP")
    static Map<Point, TerrainBoolCache> toroidal$boolCache() {
        throw new AssertionError();
    }

    @Accessor("POS")
    static Point toroidal$pos() {
        throw new AssertionError();
    }

    @Invoker("getAverageDepth")
    static float toroidal$averageDepth(int x, int z) {
        throw new AssertionError();
    }
}
