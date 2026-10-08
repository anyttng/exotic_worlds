package com.exoticworlds.engine.noise;

import com.exoticworlds.accessors.CoastLiftCache;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WrapDomain;

import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.densityfunction.DensityBuffer;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import net.minecraft.world.level.levelgen.densityfunction.DistanceMetric;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.densityfunction.op.BinaryFunction;
import net.minecraft.world.level.levelgen.densityfunction.op.ClampFunction;
import net.minecraft.world.level.levelgen.densityfunction.op.LerpFunction;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.Noise;
import net.minecraft.world.level.levelgen.synth.NoiseStack;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

public final class FoldedSamplers {
    private static final float SHIFT_AMPLITUDE = (float) NoiseConstants.SHIFT_AMPLITUDE;

    private static final double NO_Y_SCALE = 0.0;

    private static final float BLEND_CHOICE_OFFSET = 0.5F;

    private static final float BLEND_CHOICE_MIN = 0.0F;

    private static final float BLEND_CHOICE_MAX = 1.0F;

    private static final int END_ISLAND_RANDOM_SKIP = 17292;

    private static final CoastLiftCache NO_LIFT = new CoastLiftCache() {
        @Override
        public double toroidal$coastLift() {
            return 0.0;
        }

        @Override
        public void toroidal$coastLift(double lift) {
            throw new UnsupportedOperationException("A field outside the coast carries no lift");
        }
    };

    public static NoiseStack stack(FoldedCompileContext context, Holder<NormalNoise> noise) {
        return stackOf(context.createNoiseSampler(noise));
    }

    public static DensitySampler noise(FoldedCompileContext context, NoiseStack stack, double xzScale, double yScale,
            double[] layerFactors, boolean coast) {
        return new NoiseSampler(PeriodicOctaveSampler.compile(context.fold(), frameOf(context, xzScale, yScale),
                xzScale, layerFactors, stack), yScale, liftOf(context, coast));
    }

    public static DensitySampler shiftedNoise(FoldedCompileContext context, NoiseStack stack, double xzScale,
            double yScale, double[] layerFactors, boolean coast, double warpDivisor, DensityFunction shiftX,
            DensityFunction shiftY, DensityFunction shiftZ) {
        return new ShiftedNoiseSampler(context.fold(), PeriodicOctaveSampler.compile(context.fold(),
                frameOf(context, xzScale, yScale), xzScale, layerFactors, stack), xzScale, yScale,
                liftOf(context, coast), warpDivisor,
                shiftX.compileSampler(context), shiftY.compileSampler(context), shiftZ.compileSampler(context));
    }

    private static NoiseFrame frameOf(FoldedCompileContext context, double xzScale, double yScale) {
        return frameOf(context, SlotAxes.DEFAULT, GenerationTransformerContext.verticalShare(xzScale, yScale));
    }

    private static NoiseFrame frameOf(FoldedCompileContext context, SlotAxes axes, double verticalShare) {
        return new NoiseFrame(axes, context.xDivisor(), context.zDivisor(), verticalShare);
    }

    private static CoastLiftCache liftOf(FoldedCompileContext context, boolean coast) {
        return coast ? context.coastLift() : NO_LIFT;
    }

    public static DensitySampler shift(FoldedCompileContext context, Holder<NormalNoise> noise) {
        return new ShiftSampler(shiftOctaves(context, noise, SlotAxes.DEFAULT), NoiseConstants.SHIFT_SCALE, false);
    }

    public static DensitySampler shiftA(FoldedCompileContext context, Holder<NormalNoise> noise) {
        return new ShiftSampler(shiftOctaves(context, noise, SlotAxes.DEFAULT), NO_Y_SCALE, false);
    }

    public static DensitySampler shiftB(FoldedCompileContext context, Holder<NormalNoise> noise) {
        return new ShiftSampler(shiftOctaves(context, noise, DensityFunctionSlotAxes.SHIFT_B), NO_Y_SCALE, true);
    }

    private static PeriodicOctaves shiftOctaves(FoldedCompileContext context, Holder<NormalNoise> noise,
            SlotAxes axes) {
        return PeriodicOctaveSampler.compile(context.fold(),
                frameOf(context, axes, GenerationTransformerContext.UNDECLARED_VERTICAL_SHARE),
                NoiseConstants.SHIFT_SCALE, null, stackOf(context.createNoiseSampler(noise)));
    }

    public static DensitySampler blended(FoldedCompileContext context, BlendedNoise blended) {
        BlendedNoise.FbmSet fbms = blended.createFbmSet(context.createRandom(BlendedNoise.NOISE_SEED));
        double xzMultiplier = blended.xzMultiplier();
        double yMultiplier = blended.yMultiplier();
        WorldFold fold = context.fold();
        NoiseFrame frame = frameOf(context, SlotAxes.DEFAULT, GenerationTransformerContext.UNDECLARED_VERTICAL_SHARE);
        DensitySampler minLimit = new NoiseSampler(
                PeriodicOctaveSampler.compile(fold, frame, xzMultiplier, null, fbms.minLimitNoise()), yMultiplier,
                NO_LIFT);
        DensitySampler maxLimit = new NoiseSampler(
                PeriodicOctaveSampler.compile(fold, frame, xzMultiplier, null, fbms.maxLimitNoise()), yMultiplier,
                NO_LIFT);
        DensitySampler main = new NoiseSampler(
                PeriodicOctaveSampler.compile(fold, frame, xzMultiplier / blended.xzFactor(), null, fbms.mainNoise()),
                yMultiplier / blended.yFactor(), NO_LIFT);
        DensitySampler choice = new ClampFunction.Sampler(
                new BinaryFunction.ConstAddSampler(main, BLEND_CHOICE_OFFSET), BLEND_CHOICE_MIN, BLEND_CHOICE_MAX);
        return new LerpFunction.Sampler(choice, minLimit, maxLimit);
    }

    @SuppressWarnings("deprecation")
    public static DensitySampler endIslands(FoldedCompileContext context) {
        RandomSource islandRandom = context.createEndIslandRandom();
        islandRandom.consumeCount(END_ISLAND_RANDOM_SKIP);
        return new EndIslandSampler(context.fold(), new SimplexNoise(islandRandom, true));
    }

    public static DensitySampler distanceToPoint(FoldedCompileContext context, Vec3i point, DistanceMetric metric) {
        return new DistanceSampler(context.fold(), point, metric);
    }

    static NoiseStack stackOf(Noise noise) {
        if (!(noise instanceof NoiseStack stack)) {
            throw new IllegalArgumentException("A folded noise samples a NoiseStack, got " + noise.getClass().getName());
        }

        return stack;
    }

    private record NoiseSampler(PeriodicOctaves octaves, double yScale, CoastLiftCache coastLift)
            implements DensitySampler {
        @Override
        public void sampleVolume(SamplerContext context, DensityBuffer outputBuffer, DensityVolume volume) {
            DensitySampler.sampleVolumeNaive(context, outputBuffer, volume, this);
        }

        @Override
        public float sampleValue(SamplerContext context, int blockX, int blockY, int blockZ) {
            return this.octaves.sample(blockX, blockY * this.yScale, blockZ)
                    + (float) this.coastLift.toroidal$coastLift();
        }
    }

    private record ShiftedNoiseSampler(WorldFold fold, PeriodicOctaves octaves, double xzScale, double yScale,
            CoastLiftCache coastLift, double warpDivisor, DensitySampler shiftX, DensitySampler shiftY,
            DensitySampler shiftZ) implements DensitySampler {
        @Override
        public void sampleVolume(SamplerContext context, DensityBuffer outputBuffer, DensityVolume volume) {
            DensitySampler.sampleVolumeNaive(context, outputBuffer, volume, this);
        }

        @Override
        public float sampleValue(SamplerContext context, int blockX, int blockY, int blockZ) {
            double x = blockX;
            double y = blockY * this.yScale + this.shiftY.sampleValue(context, blockX, blockY, blockZ);
            double z = blockZ;
            if (this.xzScale != 0.0) {
                WrapDomain xDomain = this.fold.blockDomain(Direction.Axis.X);
                WrapDomain zDomain = this.fold.blockDomain(Direction.Axis.Z);
                x = DomainWarp.apply(xDomain, blockX, this.shiftX.sampleValue(context, blockX, blockY, blockZ),
                        this.warpDivisor);
                z = DomainWarp.apply(zDomain, blockZ, this.shiftZ.sampleValue(context, blockX, blockY, blockZ),
                        this.warpDivisor);
            }

            return this.octaves.sample(x, y, z) + (float) this.coastLift.toroidal$coastLift();
        }
    }

    private record ShiftSampler(PeriodicOctaves octaves, double yScale, boolean transposed) implements DensitySampler {
        @Override
        public void sampleVolume(SamplerContext context, DensityBuffer outputBuffer, DensityVolume volume) {
            DensitySampler.sampleVolumeNaive(context, outputBuffer, volume, this);
        }

        @Override
        public float sampleValue(SamplerContext context, int blockX, int blockY, int blockZ) {
            float value = this.transposed
                    ? this.octaves.sample(blockZ, blockX, 0.0)
                    : this.octaves.sample(blockX, blockY * this.yScale, blockZ);
            return value * SHIFT_AMPLITUDE;
        }
    }

    private record EndIslandSampler(WorldFold fold, SimplexNoise islandNoise) implements DensitySampler {
        @Override
        public void sampleVolume(SamplerContext context, DensityBuffer outputBuffer, DensityVolume volume) {
            for (int z = 0; z < volume.sizeZ(); z++) {
                int blockZ = volume.blockZ(z);
                for (int x = 0; x < volume.sizeX(); x++) {
                    float value = this.sampleValue(context, volume.blockX(x), 0, blockZ);
                    outputBuffer.setRange(volume.indexUnchecked(x, 0, z), volume.sizeY(), value);
                }
            }
        }

        @Override
        public float sampleValue(SamplerContext context, int blockX, int blockY, int blockZ) {
            return PeriodicEndIslands.density(
                    PeriodicEndIslands.outerHeightValue(this.islandNoise, this.fold, blockX, blockZ));
        }
    }

    private record DistanceSampler(WorldFold fold, Vec3i point, DistanceMetric metric) implements DensitySampler {
        @Override
        public void sampleVolume(SamplerContext context, DensityBuffer outputBuffer, DensityVolume volume) {
            DensitySampler.sampleVolumeNaive(context, outputBuffer, volume, this);
        }

        @Override
        public float sampleValue(SamplerContext context, int blockX, int blockY, int blockZ) {
            WrapDomain xDomain = this.fold.blockDomain(Direction.Axis.X);
            WrapDomain zDomain = this.fold.blockDomain(Direction.Axis.Z);
            int deltaX = xDomain.foldDelta(xDomain.wrap(this.point.getX()) - xDomain.wrap(blockX));
            int deltaZ = zDomain.foldDelta(zDomain.wrap(this.point.getZ()) - zDomain.wrap(blockZ));
            return this.metric.compute(deltaX, this.point.getY() - blockY, deltaZ);
        }
    }

    private FoldedSamplers() {
    }
}
