package com.exoticworlds.engine.noise;

import java.util.HashMap;
import java.util.Map;

import com.exoticworlds.accessors.CoastLiftCache;
import com.exoticworlds.core.WorldFold;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.Noise;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

final class BlendedNoiseFixture {
    private static final String UNUSED_BY_BLENDED_NOISE = "blended noise draws only its own random";

    record Params(double xzScale, double yScale, double xzFactor, double yFactor, double smear) {
        static final Params OVERWORLD = new Params(0.25, 0.125, 80.0, 160.0, 8.0);

        BlendedNoise noise() {
            return new BlendedNoise(this.xzScale, this.yScale, this.xzFactor, this.yFactor, this.smear);
        }
    }

    static long mix(long seed) {
        long mixed = seed;
        mixed = (mixed ^ (mixed >>> 30)) * 0xBF58476D1CE4E5B9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94D049BB133111EBL;
        return mixed ^ (mixed >>> 31);
    }

    @SuppressWarnings("ArrayRecordComponent")
    record Octave(PerlinNoise vanilla, byte[] permutations, double xo, double yo, double zo) {
        static Octave of(long seed) {
            PerlinNoise vanilla = new PerlinNoise(new LegacyRandomSource(seed));
            return new Octave(vanilla, vanilla.perms, vanilla.offsetX, vanilla.offsetY, vanilla.offsetZ);
        }
    }

    record Replica(long seed, Map<Object, DensitySampler> samplers) {
        static Replica of(long seed) {
            return new Replica(seed, new HashMap<>());
        }

        RandomSource random() {
            return new LegacyRandomSource(this.seed);
        }
    }

    private record FoldedKey(Params params, WorldFold fold) {
    }

    private static final CoastLiftCache NO_LIFT = new CoastLiftCache() {
        @Override
        public double toroidal$coastLift() {
            return 0.0;
        }

        @Override
        public void toroidal$coastLift(double lift) {
        }
    };

    static double vanilla(Replica replica, Params params, double blockX, double blockY, double blockZ) {
        return replica.samplers().computeIfAbsent(params, key -> params.noise().compileSampler(replica.random()))
                .sampleValue(SamplerContext.EMPTY_UNCACHED, Mth.floor(blockX), Mth.floor(blockY), Mth.floor(blockZ));
    }

    static double folded(Replica replica, Params params, WorldFold fold,
            double blockX, double blockY, double blockZ) {
        return replica.samplers().computeIfAbsent(new FoldedKey(params, fold), key -> FoldedSamplers.blended(
                        new FoldedCompileContext(contextOf(replica), fold, NO_LIFT), params.noise()))
                .sampleValue(SamplerContext.EMPTY_UNCACHED, Mth.floor(blockX), Mth.floor(blockY), Mth.floor(blockZ));
    }

    private static DensityFunction.CompileContext contextOf(Replica replica) {
        return new DensityFunction.CompileContext() {
            @Override
            public Noise createNoiseSampler(Holder<NormalNoise> parameters) {
                throw new UnsupportedOperationException(UNUSED_BY_BLENDED_NOISE);
            }

            @Override
            public RandomSource createRandom(Identifier seed) {
                return replica.random();
            }

            @Override
            @Deprecated
            public RandomSource createEndIslandRandom() {
                throw new UnsupportedOperationException(UNUSED_BY_BLENDED_NOISE);
            }
        };
    }

    static double sample(byte[] permutations, double xOffset, double yOffset, double zOffset,
            WorldFold fold, double scale, double x, double y, double z, double verticalShare) {
        return PeriodicNoiseSampler.sample(permutations, xOffset, yOffset, zOffset, fold, frameOf(verticalShare),
                scale, x, y, z);
    }

    static double sample(byte[] permutations, double xOffset, double yOffset, double zOffset,
            WorldFold fold, double scale, double x, double y, double z, double verticalShare, LapFloor floor) {
        return PeriodicNoiseSampler.sample(permutations, xOffset, yOffset, zOffset, fold, frameOf(verticalShare),
                scale, x, y, z, floor);
    }

    private static NoiseFrame frameOf(double verticalShare) {
        return new NoiseFrame(SlotAxes.DEFAULT, NoiseConstants.UNDIVIDED, NoiseConstants.UNDIVIDED, verticalShare);
    }

    private BlendedNoiseFixture() {
    }
}
