package com.exoticworlds.compat.betterend.mixin;

import java.awt.Point;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.betterx.betterend.noise.OpenSimplexNoise;
import org.betterx.betterend.world.generator.IslandLayer;
import org.betterx.betterend.world.generator.TerrainBoolCache;
import org.betterx.betterend.world.generator.TerrainGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(TerrainGenerator.class)
public interface TerrainGeneratorAccessor {
    @Accessor("LOCKER")
    static ReentrantLock exotic_worlds$locker() {
        throw new AssertionError();
    }

    @Accessor("largeIslands")
    static IslandLayer exotic_worlds$largeIslands() {
        throw new AssertionError();
    }

    @Accessor("mediumIslands")
    static IslandLayer exotic_worlds$mediumIslands() {
        throw new AssertionError();
    }

    @Accessor("smallIslands")
    static IslandLayer exotic_worlds$smallIslands() {
        throw new AssertionError();
    }

    @Accessor("noise1")
    static OpenSimplexNoise exotic_worlds$noise1() {
        throw new AssertionError();
    }

    @Accessor("noise2")
    static OpenSimplexNoise exotic_worlds$noise2() {
        throw new AssertionError();
    }

    @Accessor("TERRAIN_BOOL_CACHE_MAP")
    static Map<Point, TerrainBoolCache> exotic_worlds$boolCache() {
        throw new AssertionError();
    }

    @Accessor("POS")
    static Point exotic_worlds$pos() {
        throw new AssertionError();
    }

    @Invoker("getAverageDepth")
    static float exotic_worlds$averageDepth(int x, int z) {
        throw new AssertionError();
    }
}
