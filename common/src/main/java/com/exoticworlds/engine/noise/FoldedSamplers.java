package com.exoticworlds.engine.noise;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.feature.NoiseMoment;
import com.exoticworlds.core.feature.NoiseOffset;
import com.exoticworlds.core.fold.TranslationLattice;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.cuboid.CuboidAtlas;
import com.exoticworlds.engine.noise.cuboid.CuboidEndIslands;
import com.exoticworlds.engine.noise.cuboid.CuboidOctaves;
import com.exoticworlds.engine.noise.cuboid.CuboidSimplexStandIn;
import com.exoticworlds.engine.noise.cuboid.CuboidWarp;
import com.exoticworlds.engine.noise.periodic.PeriodicOctaveSampler;
import com.exoticworlds.engine.noise.periodic.SimplexLattice;

import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.densityfunction.CachingDensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.DensityBuffer;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunctions;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import net.minecraft.world.level.levelgen.densityfunction.DistanceMetric;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.densityfunction.op.BinaryFunction;
import net.minecraft.world.level.levelgen.densityfunction.op.ClampFunction;
import net.minecraft.world.level.levelgen.densityfunction.op.LerpFunction;
import net.minecraft.world.level.levelgen.densityfunction.op.SliceFunction;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.Noise;
import net.minecraft.world.level.levelgen.synth.NoiseStack;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

public final class FoldedSamplers {
    private static final float SHIFT_AMPLITUDE_BLOCKS = (float) NoiseConstants.SHIFT_AMPLITUDE_BLOCKS;

    private static final double NO_Y_SCALE = 0.0;

    private static final double WARP_DIAGONAL = Math.sqrt(2.0);

    private static final float BLEND_CHOICE_OFFSET = 0.5F;

    private static final float BLEND_CHOICE_MIN = 0.0F;

    private static final float BLEND_CHOICE_MAX = 1.0F;

    private static final int END_ISLAND_RANDOM_SKIP = 17292;


    public static NoiseStack stack(FoldedCompileContext context, Holder<NormalNoise> noise) {
        return getStack(context.vanilla().createNoiseSampler(noise));
    }

    public static DensitySampler compileNoise(FoldedCompileContext context, Holder<NormalNoise> noise,
            double vanillaXzScale, double yScale, DensityFunction shiftX, DensityFunction shiftY,
            DensityFunction shiftZ) {
        CuboidAtlas atlas = context.fold().getCuboidAtlas();
        if (atlas != null) {
            return cuboidNoise(context, atlas, stack(context, noise),
                    context.ladder().getSeparated(noise, vanillaXzScale), vanillaXzScale, yScale, shiftX, shiftY,
                    shiftZ);
        }

        double xzScale = context.ladder().getSeparated(noise, vanillaXzScale);
        double verticalShare = GenerationFoldContext.verticalShare(xzScale, yScale);
        NoiseStack stack = stack(context, noise);
        NoiseMoment moment = new NoiseMoment(context.fold(), context.dimension(), noise,
                context.routerFields().getFields(noise), stack, xzScale, verticalShare);
        double[] layerFactors = NoiseFeatures.layerFactors(context.noiseFeatures(), moment);
        NoiseOffset offset = NoiseFeatures.offsetOf(context.noiseFeatures(), context.offsets(), moment);
        DensityFunction zero = DensityFunctions.zero();
        if (shiftX.equals(zero) && shiftY.equals(zero) && shiftZ.equals(zero)) {
            return noise(context, stack, xzScale, vanillaXzScale, yScale, layerFactors, offset);
        }

        return shiftedNoise(context, stack, xzScale, vanillaXzScale, yScale, layerFactors, offset,
                NoiseFeatures.warpDivisor(context.noiseFeatures(), moment), shiftX, shiftY, shiftZ);
    }

    private static DensitySampler cuboidNoise(FoldedCompileContext context, CuboidAtlas atlas, NoiseStack stack,
            double xzScale, double vanillaXzScale, double yScale, DensityFunction shiftX, DensityFunction shiftY,
            DensityFunction shiftZ) {
        CompiledOctaves octaves = octaves(context.fold(), frameOf(context, xzScale, yScale),
                xzScale, null, stack);
        CuboidWarp warp = octaves instanceof CuboidOctaves
                ? cuboidWarp(context, atlas, vanillaXzScale, shiftX, shiftZ)
                : null;
        if (warp != null) {
            return new CuboidShiftedNoiseSampler((CuboidOctaves) octaves, warp, yScale,
                    shiftY.compileSampler(context.vanilla()));
        }

        return shiftY.equals(DensityFunctions.zero())
                ? new NoiseSampler(octaves, yScale, NoiseOffset.NONE)
                : new RaisedNoiseSampler(octaves, yScale, shiftY.compileSampler(context.vanilla()));
    }

    private static @Nullable CuboidWarp cuboidWarp(FoldedCompileContext context, CuboidAtlas atlas, double xzScale,
            DensityFunction shiftX, DensityFunction shiftZ) {
        DensityFunction zero = DensityFunctions.zero();
        if (xzScale == 0.0 || shiftX.equals(zero) || shiftZ.equals(zero) || !context.boxDivisors().isUndivided()) {
            return null;
        }

        if (!(shiftSource(shiftX.compileSampler(context.vanilla())) instanceof CuboidShiftSampler alongX)
                || !(shiftSource(shiftZ.compileSampler(context.vanilla())) instanceof CuboidShiftSampler alongZ)) {
            return null;
        }

        if (alongX.isTransposed() || !alongZ.isTransposed() || !alongX.noise().equals(alongZ.noise())
                || alongX.range() <= 0.0F) {
            return null;
        }

        double shiftBlocks = SHIFT_AMPLITUDE_BLOCKS / xzScale;
        return CuboidWarp.of(atlas, alongX.octaves(), shiftBlocks,
                Math.abs(shiftBlocks) * alongX.range() * WARP_DIAGONAL);
    }

    private static DensitySampler shiftSource(DensitySampler sampler) {
        return switch (sampler) {
            case CachingDensitySampler cached -> shiftSource(cached.input());
            case SliceFunction.YSampler slice -> shiftSource(slice.input());
            default -> sampler;
        };
    }

    private static DensitySampler noise(FoldedCompileContext context, NoiseStack stack, double xzScale,
            double vanillaXzScale, double yScale, double[] layerFactors, NoiseOffset offset) {
        return octavesOrVanilla(context, octaves(context.fold(),
                frameOf(context, xzScale, yScale), xzScale, layerFactors, stack), stack, vanillaXzScale, yScale,
                offset);
    }

    private static DensitySampler octavesOrVanilla(FoldedCompileContext context, CompiledOctaves octaves,
            NoiseStack stack, double vanillaXzScale, double yScale, NoiseOffset offset) {
        if (octaves.isIndexable()) {
            return new NoiseSampler(octaves, yScale, offset);
        }

        DensitySampler zero = DensityFunctions.zero().compileSampler(context.vanilla());
        return new VanillaAtFoldSampler(context.fold(), stack, vanillaXzScale, yScale, offset, zero, zero, zero);
    }

    private static DensitySampler shiftedNoise(FoldedCompileContext context, NoiseStack stack, double xzScale,
            double vanillaXzScale, double yScale, double[] layerFactors, NoiseOffset offset, double warpDivisor,
            DensityFunction shiftX, DensityFunction shiftY, DensityFunction shiftZ) {
        CompiledOctaves octaves = octaves(context.fold(), frameOf(context, xzScale, yScale),
                xzScale, layerFactors, stack);
        DensitySampler shiftXSampler = shiftX.compileSampler(context.vanilla());
        DensitySampler shiftYSampler = shiftY.compileSampler(context.vanilla());
        DensitySampler shiftZSampler = shiftZ.compileSampler(context.vanilla());
        if (!octaves.isIndexable()) {
            return new VanillaAtFoldSampler(context.fold(), stack, vanillaXzScale, yScale, offset,
                    shiftXSampler, shiftYSampler, shiftZSampler);
        }

        return new ShiftedNoiseSampler(context.fold(), octaves, stack, xzScale, vanillaXzScale, yScale,
                offset, warpDivisor, shiftXSampler, shiftYSampler, shiftZSampler);
    }

    public static CompiledOctaves octaves(WorldFold fold, NoiseFrame frame, double scale,
            double @Nullable [] layerFactors, NoiseStack stack) {
        CuboidAtlas atlas = fold.getCuboidAtlas();
        return atlas != null && frame.isDefault()
                ? CuboidOctaves.compile(atlas, scale, frame.boxDivisors(), layerFactors, stack)
                : PeriodicOctaveSampler.compile(fold, frame, scale, layerFactors, stack);
    }

    public static SimplexField simplexField(WorldFold fold, double scale, SimplexNoise noise) {
        CuboidAtlas atlas = fold.getCuboidAtlas();
        return atlas != null
                ? CuboidSimplexStandIn.of(fold, atlas, scale, noise)
                : SimplexLattice.of(fold, scale);
    }

    private static NoiseFrame frameOf(FoldedCompileContext context, double xzScale, double yScale) {
        return frameOf(context, SlotAxes.DEFAULT, GenerationFoldContext.verticalShare(xzScale, yScale));
    }

    private static NoiseFrame frameOf(FoldedCompileContext context, SlotAxes axes, double verticalShare) {
        return new NoiseFrame(axes, context.xDivisor(), context.zDivisor(), verticalShare, context.boxDivisors());
    }

    public static DensitySampler shift(FoldedCompileContext context, Holder<NormalNoise> noise) {
        return shiftSampler(context, noise, SlotAxes.DEFAULT, NoiseConstants.SHIFT_SCALE, false);
    }

    public static DensitySampler shiftA(FoldedCompileContext context, Holder<NormalNoise> noise) {
        return shiftSampler(context, noise, SlotAxes.DEFAULT, NO_Y_SCALE, false);
    }

    public static DensitySampler shiftB(FoldedCompileContext context, Holder<NormalNoise> noise) {
        boolean cuboid = context.fold().getCuboidAtlas() != null
                && frameOf(context, SlotAxes.DEFAULT, GenerationFoldContext.UNDECLARED_VERTICAL_SHARE).isDefault();
        return shiftSampler(context, noise, cuboid ? SlotAxes.DEFAULT : DensityFunctionSlotAxes.SHIFT_B, NO_Y_SCALE,
                true);
    }

    private static DensitySampler shiftSampler(FoldedCompileContext context, Holder<NormalNoise> noise,
            SlotAxes axes, double yScale, boolean isTransposed) {
        NoiseStack stack = getStack(context.vanilla().createNoiseSampler(noise));
        CompiledOctaves octaves = octaves(context.fold(),
                frameOf(context, axes, GenerationFoldContext.UNDECLARED_VERTICAL_SHARE),
                NoiseConstants.SHIFT_SCALE, null, stack);
        if (octaves instanceof CuboidOctaves cuboid && yScale == NO_Y_SCALE) {
            return new CuboidShiftSampler(noise, isTransposed, cuboid, stack.range().max());
        }

        return octaves.isIndexable()
                ? new ShiftSampler(octaves, yScale, isTransposed)
                : new VanillaShiftAtFoldSampler(context.fold(), stack, yScale, isTransposed);
    }

    public static DensitySampler blended(FoldedCompileContext context, BlendedNoise blended) {
        BlendedNoise.FbmSet fbms = blended.createFbmSet(context.vanilla().createRandom(BlendedNoise.NOISE_SEED));
        double xzMultiplier = blended.xzMultiplier();
        double yMultiplier = blended.yMultiplier();
        double mainXzScale = xzMultiplier / blended.xzFactor();
        WorldFold fold = context.fold();
        NoiseFrame frame = frameOf(context, SlotAxes.DEFAULT, GenerationFoldContext.UNDECLARED_VERTICAL_SHARE);
        DensitySampler minLimit = octavesOrVanilla(context,
                octaves(fold, frame, xzMultiplier, null, fbms.minLimitNoise()),
                fbms.minLimitNoise(), xzMultiplier, yMultiplier, NoiseOffset.NONE);
        DensitySampler maxLimit = octavesOrVanilla(context,
                octaves(fold, frame, xzMultiplier, null, fbms.maxLimitNoise()),
                fbms.maxLimitNoise(), xzMultiplier, yMultiplier, NoiseOffset.NONE);
        DensitySampler main = octavesOrVanilla(context,
                octaves(fold, frame, mainXzScale, null, fbms.mainNoise()),
                fbms.mainNoise(), mainXzScale, yMultiplier / blended.yFactor(), NoiseOffset.NONE);
        DensitySampler choice = new ClampFunction.Sampler(
                new BinaryFunction.ConstAddSampler(main, BLEND_CHOICE_OFFSET), BLEND_CHOICE_MIN, BLEND_CHOICE_MAX);
        return new LerpFunction.Sampler(choice, minLimit, maxLimit);
    }

    @SuppressWarnings("deprecation")
    public static DensitySampler endIslands(FoldedCompileContext context) {
        RandomSource islandRandom = context.vanilla().createEndIslandRandom();
        islandRandom.consumeCount(END_ISLAND_RANDOM_SKIP);
        SimplexNoise islandNoise = new SimplexNoise(islandRandom, true);
        WorldFold fold = context.fold();
        CuboidAtlas atlas = fold.getCuboidAtlas();
        if (atlas != null) {
            CuboidEndIslands islands = CuboidEndIslands.of(atlas);
            return new EndIslandSampler((blockX, blockZ) -> islands.outerHeightValue(islandNoise, blockX, blockZ));
        }

        return new EndIslandSampler(
                (blockX, blockZ) -> PeriodicEndIslands.outerHeightValue(islandNoise, fold, blockX, blockZ));
    }

    public static DensitySampler distanceToPoint(FoldedCompileContext context, Vec3i point, DistanceMetric metric) {
        return new DistanceSampler(context.fold(), point, metric);
    }

    static NoiseStack getStack(Noise noise) {
        if (!(noise instanceof NoiseStack stack)) {
            throw new IllegalArgumentException("A folded noise samples a NoiseStack, got " + noise.getClass().getName());
        }

        return stack;
    }

    private record NoiseSampler(CompiledOctaves octaves, double yScale, NoiseOffset offset)
            implements DensitySampler {
        @Override
        public void sampleVolume(SamplerContext context, DensityBuffer outputBuffer, DensityVolume volume) {
            DensitySampler.sampleVolumeNaive(context, outputBuffer, volume, this);
        }

        @Override
        public float sampleValue(SamplerContext context, int blockX, int blockY, int blockZ) {
            return this.octaves.sample(blockX, blockY * this.yScale, blockZ)
                    + (float) this.offset.get();
        }
    }

    private record RaisedNoiseSampler(CompiledOctaves octaves, double yScale, DensitySampler shiftY)
            implements DensitySampler {
        @Override
        public void sampleVolume(SamplerContext context, DensityBuffer outputBuffer, DensityVolume volume) {
            DensitySampler.sampleVolumeNaive(context, outputBuffer, volume, this);
        }

        @Override
        public float sampleValue(SamplerContext context, int blockX, int blockY, int blockZ) {
            return this.octaves.sample(blockX,
                    blockY * this.yScale + this.shiftY.sampleValue(context, blockX, blockY, blockZ), blockZ);
        }
    }

    private record ShiftedNoiseSampler(WorldFold fold, CompiledOctaves octaves, Noise noise, double xzScale,
            double vanillaXzScale, double yScale, NoiseOffset offset, double warpDivisor, DensitySampler shiftX,
            DensitySampler shiftY, DensitySampler shiftZ) implements DensitySampler {
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
                TranslationLattice lattice = this.fold.getBlockLattice();
                double shiftX = this.shiftX.sampleValue(context, blockX, blockY, blockZ);
                double shiftZ = this.shiftZ.sampleValue(context, blockX, blockY, blockZ);
                if (!DomainWarp.canCarry(lattice, blockX, blockZ, shiftX, shiftZ, this.vanillaXzScale)) {
                    return vanillaAtFold(lattice, this.noise, this.vanillaXzScale, blockX, y, blockZ, shiftX, shiftZ)
                            + (float) this.offset.get();
                }

                x = DomainWarp.applyX(lattice, blockX, blockZ, shiftX, this.warpDivisor);
                z = DomainWarp.applyZ(lattice, blockZ, shiftZ, this.warpDivisor);
            }

            return this.octaves.sample(x, y, z) + (float) this.offset.get();
        }
    }

    private record VanillaAtFoldSampler(WorldFold fold, Noise noise, double xzScale, double yScale,
            NoiseOffset offset, DensitySampler shiftX, DensitySampler shiftY, DensitySampler shiftZ)
            implements DensitySampler {
        @Override
        public void sampleVolume(SamplerContext context, DensityBuffer outputBuffer, DensityVolume volume) {
            DensitySampler.sampleVolumeNaive(context, outputBuffer, volume, this);
        }

        @Override
        public float sampleValue(SamplerContext context, int blockX, int blockY, int blockZ) {
            double y = blockY * this.yScale + this.shiftY.sampleValue(context, blockX, blockY, blockZ);
            return vanillaAtFold(this.fold.getBlockLattice(), this.noise, this.xzScale, blockX, y, blockZ,
                    this.shiftX.sampleValue(context, blockX, blockY, blockZ),
                    this.shiftZ.sampleValue(context, blockX, blockY, blockZ))
                    + (float) this.offset.get();
        }
    }

    static float vanillaAtFold(TranslationLattice lattice, Noise noise, double xzScale, double x, double y,
            double z, double shiftX, double shiftZ) {
        return noise.get(lattice.foldX(x, z) * xzScale + shiftX, y, lattice.foldZ(z) * xzScale + shiftZ);
    }

    private record VanillaShiftAtFoldSampler(WorldFold fold, Noise noise, double yScale, boolean isTransposed)
            implements DensitySampler {
        @Override
        public void sampleVolume(SamplerContext context, DensityBuffer outputBuffer, DensityVolume volume) {
            DensitySampler.sampleVolumeNaive(context, outputBuffer, volume, this);
        }

        @Override
        public float sampleValue(SamplerContext context, int blockX, int blockY, int blockZ) {
            TranslationLattice lattice = this.fold.getBlockLattice();
            double x = lattice.foldX(blockX, blockZ) * NoiseConstants.SHIFT_SCALE;
            double z = lattice.foldZ(blockZ) * NoiseConstants.SHIFT_SCALE;
            float value = this.isTransposed
                    ? this.noise.get(z, x, 0.0)
                    : this.noise.get(x, blockY * this.yScale, z);
            return value * SHIFT_AMPLITUDE_BLOCKS;
        }
    }

    private record ShiftSampler(CompiledOctaves octaves, double yScale, boolean isTransposed)
            implements DensitySampler {
        @Override
        public void sampleVolume(SamplerContext context, DensityBuffer outputBuffer, DensityVolume volume) {
            DensitySampler.sampleVolumeNaive(context, outputBuffer, volume, this);
        }

        @Override
        public float sampleValue(SamplerContext context, int blockX, int blockY, int blockZ) {
            float value = this.isTransposed
                    ? this.octaves.sample(blockZ, blockX, 0.0)
                    : this.octaves.sample(blockX, blockY * this.yScale, blockZ);
            return value * SHIFT_AMPLITUDE_BLOCKS;
        }
    }

    private record CuboidShiftSampler(Holder<NormalNoise> noise, boolean isTransposed, CuboidOctaves octaves,
            float range) implements DensitySampler {
        @Override
        public void sampleVolume(SamplerContext context, DensityBuffer outputBuffer, DensityVolume volume) {
            DensitySampler.sampleVolumeNaive(context, outputBuffer, volume, this);
        }

        @Override
        public float sampleValue(SamplerContext context, int blockX, int blockY, int blockZ) {
            float value = this.isTransposed
                    ? this.octaves.sampleSecond(blockX, NO_Y_SCALE, blockZ)
                    : this.octaves.sample(blockX, NO_Y_SCALE, blockZ);
            return value * SHIFT_AMPLITUDE_BLOCKS;
        }
    }

    private record CuboidShiftedNoiseSampler(CuboidOctaves octaves, CuboidWarp warp, double yScale,
            DensitySampler shiftY) implements DensitySampler {
        @Override
        public void sampleVolume(SamplerContext context, DensityBuffer outputBuffer, DensityVolume volume) {
            DensitySampler.sampleVolumeNaive(context, outputBuffer, volume, this);
        }

        @Override
        public float sampleValue(SamplerContext context, int blockX, int blockY, int blockZ) {
            return this.warp.sample(this.octaves, blockX,
                    blockY * this.yScale + this.shiftY.sampleValue(context, blockX, blockY, blockZ), blockZ);
        }
    }

    @FunctionalInterface
    private interface OuterIslands {
        float heightValue(int blockX, int blockZ);
    }

    private record EndIslandSampler(OuterIslands islands) implements DensitySampler {
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
            return PeriodicEndIslands.density(this.islands.heightValue(blockX, blockZ));
        }
    }

    private record DistanceSampler(WorldFold fold, Vec3i point, DistanceMetric metric) implements DensitySampler {
        @Override
        public void sampleVolume(SamplerContext context, DensityBuffer outputBuffer, DensityVolume volume) {
            DensitySampler.sampleVolumeNaive(context, outputBuffer, volume, this);
        }

        @Override
        public float sampleValue(SamplerContext context, int blockX, int blockY, int blockZ) {
            TranslationLattice lattice = this.fold.getBlockLattice();
            int deltaX = lattice.foldX(this.point.getX(), this.point.getZ()) - lattice.foldX(blockX, blockZ);
            int deltaZ = lattice.foldZ(this.point.getZ()) - lattice.foldZ(blockZ);
            int laps = lattice.nearestZLaps(deltaX, deltaZ);
            return this.metric.compute(lattice.nearestDeltaX(deltaX, laps), this.point.getY() - blockY,
                    lattice.nearestDeltaZ(deltaZ, laps));
        }
    }

    private FoldedSamplers() {
    }
}
