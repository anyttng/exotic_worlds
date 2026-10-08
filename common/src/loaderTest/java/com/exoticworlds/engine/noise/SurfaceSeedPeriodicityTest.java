package com.exoticworlds.engine.noise;

import static com.exoticworlds.engine.noise.DensityFunctionFixture.SEED;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.WORLDS;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.blockIn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.exoticworlds.core.WorldFold;

import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.material.MaterialSystem;

class SurfaceSeedPeriodicityTest {
    private static final int SAMPLES = 256;
    private static final int MIN_DISTINCT_DEPTHS = 2;

    private static final String SURFACE_DEPTH = "getSurfaceDepth";

    private static NoiseGeneratorSettings settings;

    private static Method surfaceDepth;

    @BeforeAll
    static void bootstrapVanilla() throws NoSuchMethodException {
        ClimateScanFixture.bootstrapVanilla();
        settings = ClimateScanFixture.settingsOf(ClimateScanFixture.TYPES.getFirst());
        surfaceDepth = MaterialSystem.class.getDeclaredMethod(SURFACE_DEPTH, int.class, int.class);
        surfaceDepth.setAccessible(true);
    }

    @Test
    void surfaceDepthAgreesOneWorldWidthApartInX() {
        for (WorldFold fold : WORLDS) {
            MaterialSystem surface = surfaceFor(fold);
            int width = fold.blockDomain(Direction.Axis.X).domainLength;
            Random random = new Random(SEED);
            for (int i = 0; i < SAMPLES; i++) {
                int x = blockIn(random, fold.blockDomain(Direction.Axis.X));
                int z = blockIn(random, fold.blockDomain(Direction.Axis.Z));
                assertEquals(depth(surface, fold, x, z), depth(surface, fold, x + width, z),
                        "surface depth in " + fold + " at x=" + x + " vs x=" + (x + width) + ", z=" + z);
            }
        }
    }

    @Test
    void surfaceDepthAgreesOneWorldWidthApartInZ() {
        for (WorldFold fold : WORLDS) {
            MaterialSystem surface = surfaceFor(fold);
            int width = fold.blockDomain(Direction.Axis.Z).domainLength;
            Random random = new Random(SEED);
            for (int i = 0; i < SAMPLES; i++) {
                int x = blockIn(random, fold.blockDomain(Direction.Axis.X));
                int z = blockIn(random, fold.blockDomain(Direction.Axis.Z));
                assertEquals(depth(surface, fold, x, z), depth(surface, fold, x, z + width),
                        "surface depth in " + fold + " at z=" + z + " vs z=" + (z + width) + ", x=" + x);
            }
        }
    }

    @Test
    void surfaceDepthVariesAcrossTheWorld() {
        for (WorldFold fold : WORLDS) {
            MaterialSystem surface = surfaceFor(fold);
            Random random = new Random(SEED);
            Set<Integer> depths = new HashSet<>();
            for (int i = 0; i < SAMPLES; i++) {
                depths.add(depth(surface, fold, blockIn(random, fold.blockDomain(Direction.Axis.X)),
                        blockIn(random, fold.blockDomain(Direction.Axis.Z))));
            }

            assertTrue(depths.size() >= MIN_DISTINCT_DEPTHS,
                    "surface depth in " + fold + " took a single value: " + depths);
        }
    }

    private static MaterialSystem surfaceFor(WorldFold fold) {
        return ClimateScanFixture.randomState(settings, fold, SEED).surfaceSystem();
    }

    private static int depth(MaterialSystem surface, WorldFold fold, int blockX, int blockZ) {
        return GenerationTransformerContext.withTransformer(fold, () -> depthAt(surface, blockX, blockZ));
    }

    private static int depthAt(MaterialSystem surface, int blockX, int blockZ) {
        try {
            return (int) surfaceDepth.invoke(surface, blockX, blockZ);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }
}
