package com.exoticworlds.engine.noise;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.fold.cuboid.CuboidAtlas;
import com.exoticworlds.core.fold.cuboid.CuboidFace;
import com.exoticworlds.core.fold.cuboid.CuboidFixtures;
import com.exoticworlds.core.fold.WorldFold;

import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.NoiseStack;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

class CuboidSimplexCalibrationTest {
    private static final long SEED = 0x1599L;
    private static final int FACE_CHUNKS = 64;
    private static final int REALIZATIONS = 8;
    private static final double TOLERANCE = 0.03;

    private static final double LINE_SCALE = NoiseConstants.HEIGHT_TEMPERATURE_SCALE;
    private static final double STEP_UNITS = 0.01;
    private static final int LINES_PER_FACE = 16;
    private static final double FIRST_LINE_BLOCKS = 7.3;

    private static final int GRID_STEP_BLOCKS = 4;
    private static final float LARGE_VARIATION_AMPLITUDE = 7.0F;
    private static final double ICE_PATCH_THRESHOLD = 0.3;
    private static final double SMALL_VARIATION_THRESHOLD = 0.8;

    private static final CuboidAtlas ATLAS = CuboidAtlas.of(FACE_CHUNKS, FACE_CHUNKS, FACE_CHUNKS, 0, 0);
    private static final WorldFold FOLD = CuboidFixtures.cuboidFold(ATLAS);

    private interface Field {
        double at(double blockX, double blockZ);
    }

    private static final class Moments {
        private double squares;

        private long samples;

        private long crossings;

        private double units;

        void line(Field field, CuboidFace face, double z) {
            double stepBlocks = STEP_UNITS / LINE_SCALE;
            int steps = (int) (face.widthBlocks() / stepBlocks);
            double previous = field.at(face.minBlockX(), z);
            for (int step = 1; step < steps; step++) {
                double value = field.at(face.minBlockX() + step * stepBlocks, z);
                this.squares += value * value;
                this.samples++;
                if ((value > 0.0) != (previous > 0.0)) {
                    this.crossings++;
                }

                previous = value;
            }

            this.units += steps * STEP_UNITS;
        }

        double deviation() {
            return Math.sqrt(this.squares / this.samples);
        }

        double crossingsPerUnit() {
            return this.crossings / this.units;
        }
    }

    private static final class Shares {
        private long points;

        private long underIce;

        private long underSmall;

        private long patches;

        void add(double large, double edge, double small) {
            boolean ice = large + edge < ICE_PATCH_THRESHOLD;
            boolean smallUnder = small < SMALL_VARIATION_THRESHOLD;
            this.points++;
            this.underIce += ice ? 1 : 0;
            this.underSmall += smallUnder ? 1 : 0;
            this.patches += ice && smallUnder ? 1 : 0;
        }

        double underIce() {
            return (double) this.underIce / this.points;
        }

        double underSmall() {
            return (double) this.underSmall / this.points;
        }

        double patches() {
            return (double) this.patches / this.points;
        }

        String against(Shares vanilla) {
            return String.format(Locale.ROOT, "stand-in / vanilla: under the ice threshold %.4f / %.4f, "
                    + "under the small threshold %.4f / %.4f, ice patches %.4f / %.4f", underIce(),
                    vanilla.underIce(), underSmall(), vanilla.underSmall(), patches(), vanilla.patches());
        }
    }

    private record Frozen(NoiseStack stack, SimplexNoise info) {
        static Frozen of(long seed) {
            WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(seed));
            NoiseStack stack = NoiseStack.builder()
                    .add(new SimplexNoise(random, true), 1.0, 0.14285715F)
                    .add(new SimplexNoise(random, true), 0.5, 0.2857143F)
                    .add(new SimplexNoise(random, true), 0.25, 0.5714286F)
                    .build();
            return new Frozen(stack, new SimplexNoise(random, true));
        }
    }

    @Test
    void theStandInSpreadsAndCrossesZeroLikeVanillaSimplex() {
        Random seeds = new Random(SEED);
        Moments vanilla = new Moments();
        Moments standIn = new Moments();
        for (int realization = 0; realization < REALIZATIONS; realization++) {
            SimplexNoise leaf = new SimplexNoise(new LegacyRandomSource(seeds.nextLong()), true);
            SimplexSlot slot = new SimplexSlot();
            for (CuboidFace face : ATLAS.faces()) {
                double spacing = (face.depthBlocks() - 2.0 * FIRST_LINE_BLOCKS) / LINES_PER_FACE;
                for (int line = 0; line < LINES_PER_FACE; line++) {
                    double z = face.minBlockZ() + FIRST_LINE_BLOCKS + line * spacing;
                    vanilla.line((x, lineZ) -> leaf.get(x * LINE_SCALE, lineZ * LINE_SCALE), face, z);
                    standIn.line((x, lineZ) -> slot.sample(leaf, FOLD, LINE_SCALE, x, lineZ), face, z);
                }
            }
        }

        String readings = String.format(Locale.ROOT, "stand-in / vanilla: spread %.4f / %.4f, "
                + "zero crossings per unit %.4f / %.4f", standIn.deviation(), vanilla.deviation(),
                standIn.crossingsPerUnit(), vanilla.crossingsPerUnit());
        assertEquals(1.0, standIn.deviation() / vanilla.deviation(), TOLERANCE, readings);
        assertEquals(1.0, standIn.crossingsPerUnit() / vanilla.crossingsPerUnit(), TOLERANCE, readings);
    }

    @Test
    void theStandInFreezesAsOftenAsVanilla() {
        Random seeds = new Random(SEED);
        Shares vanilla = new Shares();
        Shares standIn = new Shares();
        for (int realization = 0; realization < REALIZATIONS; realization++) {
            Frozen frozen = Frozen.of(seeds.nextLong());
            SimplexSlot.Stack frozenSlot = new SimplexSlot.Stack();
            SimplexSlot edgeSlot = new SimplexSlot();
            SimplexSlot patchSlot = new SimplexSlot();
            for (CuboidFace face : ATLAS.faces()) {
                for (int localX = 0; localX < face.widthBlocks(); localX += GRID_STEP_BLOCKS) {
                    for (int localZ = 0; localZ < face.depthBlocks(); localZ += GRID_STEP_BLOCKS) {
                        int x = face.blockX(localX);
                        int z = face.blockZ(localZ);
                        vanilla.add(vanillaLarge(frozen, x, z),
                                frozen.info().get(x * NoiseConstants.BIOME_INFO_EDGE_SCALE,
                                        z * NoiseConstants.BIOME_INFO_EDGE_SCALE),
                                frozen.info().get(x * NoiseConstants.BIOME_INFO_PATCH_SCALE,
                                        z * NoiseConstants.BIOME_INFO_PATCH_SCALE));
                        standIn.add(standInLarge(frozenSlot, frozen, x, z),
                                edgeSlot.sample(frozen.info(), FOLD, NoiseConstants.BIOME_INFO_EDGE_SCALE, x, z),
                                patchSlot.sample(frozen.info(), FOLD, NoiseConstants.BIOME_INFO_PATCH_SCALE, x,
                                        z));
                    }
                }
            }
        }

        String readings = standIn.against(vanilla);
        assertEquals(vanilla.underIce(), standIn.underIce(), TOLERANCE, readings);
        assertEquals(vanilla.underSmall(), standIn.underSmall(), TOLERANCE, readings);
        assertEquals(vanilla.patches(), standIn.patches(), TOLERANCE, readings);
    }

    private static double vanillaLarge(Frozen frozen, int x, int z) {
        float large = frozen.stack().get(x * NoiseConstants.FROZEN_TEMPERATURE_SCALE,
                z * NoiseConstants.FROZEN_TEMPERATURE_SCALE);
        return large * LARGE_VARIATION_AMPLITUDE;
    }

    private static double standInLarge(SimplexSlot.Stack slot, Frozen frozen, int x, int z) {
        float large = slot.sample(FOLD, NoiseConstants.FROZEN_TEMPERATURE_SCALE, frozen.stack(), x, z);
        return large * LARGE_VARIATION_AMPLITUDE;
    }
}
