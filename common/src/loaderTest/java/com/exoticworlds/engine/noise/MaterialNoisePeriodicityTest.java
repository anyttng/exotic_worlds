package com.exoticworlds.engine.noise;

import static com.exoticworlds.engine.noise.DensityFunctionFixture.SEED;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.WORLDS;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.blockIn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.exoticworlds.core.WorldFold;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.densityfunction.DensitySamplerSet;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import net.minecraft.world.level.levelgen.material.MaterialRuleContext;
import net.minecraft.world.level.levelgen.material.MaterialSystem;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

class MaterialNoisePeriodicityTest {
    private static final int SAMPLES = 256;
    private static final int MIN_DISTINCT_VALUES = 2;

    private static final String SURFACE_SECONDARY = "getSurfaceSecondary";
    private static final String BAND = "getBand";
    private static final String UPDATE_XZ = "updateXZ";
    private static final String UPDATE_Y = "updateY";

    private static final int BAND_Y = 64;
    private static final int NO_GRADIENT = 0;
    private static final int NO_STONE_DEPTH = 1;
    private static final int NO_WATER = Integer.MIN_VALUE;

    private static final ResourceKey<NormalNoise> RULE_NOISE_2D = Noises.SURFACE;
    private static final ResourceKey<NormalNoise> RULE_NOISE_3D = Noises.CALCITE;

    private interface Read {
        Object at(MaterialSystem surface, RandomState state, int blockX, int blockZ);
    }

    private static NoiseGeneratorSettings settings;
    private static Method surfaceSecondary;
    private static Method band;
    private static Method updateXZ;
    private static Method updateY;
    private static Constructor<MaterialRuleContext> ruleContext;

    @BeforeAll
    static void bootstrapVanilla() throws NoSuchMethodException {
        ClimateScanFixture.bootstrapVanilla();
        settings = ClimateScanFixture.settingsOf(ClimateScanFixture.TYPES.getFirst());
        surfaceSecondary = accessible(MaterialSystem.class.getDeclaredMethod(SURFACE_SECONDARY, int.class, int.class));
        band = accessible(MaterialSystem.class.getDeclaredMethod(BAND, int.class, int.class, int.class));
        updateXZ = accessible(MaterialRuleContext.class.getDeclaredMethod(UPDATE_XZ,
                int.class, int.class, int.class, int.class));
        updateY = accessible(MaterialRuleContext.class.getDeclaredMethod(UPDATE_Y,
                int.class, int.class, int.class, int.class));
        ruleContext = MaterialRuleContext.class.getDeclaredConstructor(MaterialSystem.class, RandomState.class,
                DensityVolume.class, DensitySamplerSet.class, Function.class, WorldGenerationContext.class,
                Set.class);
        ruleContext.setAccessible(true);
    }

    @Test
    void surfaceSecondaryRepeatsOneLapAway() {
        assertPeriodic("surface secondary", (surface, state, x, z) -> invoke(surfaceSecondary, surface, x, z));
    }

    @Test
    void clayBandsRepeatOneLapAway() {
        assertPeriodic("clay band", (surface, state, x, z) -> invoke(band, surface, x, BAND_Y, z));
    }

    @Test
    void twoDimensionalRuleNoiseRepeatsOneLapAway() {
        assertPeriodic("2d rule noise", (surface, state, x, z) -> ruleNoise(surface, state, RULE_NOISE_2D, false, x, z));
    }

    @Test
    void threeDimensionalRuleNoiseRepeatsOneLapAway() {
        assertPeriodic("3d rule noise", (surface, state, x, z) -> ruleNoise(surface, state, RULE_NOISE_3D, true, x, z));
    }

    private static void assertPeriodic(String name, Read read) {
        for (WorldFold fold : WORLDS) {
            RandomState state = ClimateScanFixture.randomState(settings, fold, SEED);
            MaterialSystem surface = state.surfaceSystem();
            int widthX = fold.blockDomain(Direction.Axis.X).domainLength;
            int widthZ = fold.blockDomain(Direction.Axis.Z).domainLength;
            Random random = new Random(SEED);
            Set<Object> values = new HashSet<>();
            for (int i = 0; i < SAMPLES; i++) {
                int x = blockIn(random, fold.blockDomain(Direction.Axis.X));
                int z = blockIn(random, fold.blockDomain(Direction.Axis.Z));
                Object here = bound(fold, () -> read.at(surface, state, x, z));
                values.add(here);
                assertEquals(here, bound(fold, () -> read.at(surface, state, x + widthX, z)),
                        name + " in " + fold + " at x=" + x + " vs x=" + (x + widthX) + ", z=" + z);
                assertEquals(here, bound(fold, () -> read.at(surface, state, x, z + widthZ)),
                        name + " in " + fold + " at z=" + z + " vs z=" + (z + widthZ) + ", x=" + x);
            }

            assertTrue(values.size() >= MIN_DISTINCT_VALUES, name + " in " + fold + " took a single value: " + values);
        }
    }

    private static Object bound(WorldFold fold, Supplier<Object> read) {
        return GenerationTransformerContext.withTransformer(fold, read);
    }

    private static double ruleNoise(MaterialSystem surface, RandomState state, ResourceKey<NormalNoise> noise,
            boolean is3d, int blockX, int blockZ) {
        try {
            MaterialRuleContext context = ruleContext.newInstance(surface, state,
                    new DensityVolume(1, 1, 1, blockX, BAND_Y, blockZ), null, null, null, null);
            updateXZ.invoke(context, blockX, blockZ, NO_GRADIENT, NO_GRADIENT);
            updateY.invoke(context, NO_STONE_DEPTH, NO_STONE_DEPTH, NO_WATER, BAND_Y);
            DoubleSupplier sampler = context.getNoiseSampler(noise, is3d);
            return sampler.getAsDouble();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Object invoke(Method method, Object target, Object... args) {
        try {
            return method.invoke(target, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Method accessible(Method method) {
        method.setAccessible(true);
        return method;
    }
}
