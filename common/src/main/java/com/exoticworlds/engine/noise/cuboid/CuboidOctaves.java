package com.exoticworlds.engine.noise.cuboid;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.fold.cuboid.CuboidAtlas;
import com.exoticworlds.core.fold.cuboid.CuboidFace;
import com.exoticworlds.engine.noise.BoxDivisors;
import com.exoticworlds.engine.noise.CompiledOctaves;
import com.exoticworlds.engine.noise.PerlinMath;
import com.exoticworlds.engine.noise.cuboid.CuboidCrossings.Crossing;
import com.exoticworlds.engine.noise.cuboid.CuboidNoiseSampler.Part;

import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.synth.GradientNoise;
import net.minecraft.world.level.levelgen.synth.Noise;
import net.minecraft.world.level.levelgen.synth.NoiseStack;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import net.minecraft.world.level.levelgen.synth.SmearedPerlinNoise;

public final class CuboidOctaves implements CompiledOctaves {
    private static final String NOT_PERLIN_LEAF = "Cuboid octaves sample Perlin leaves only, got ";

    private static final double WARP_Y = 0.0;

    private final List<CuboidFace> faces;

    private final CuboidAskingFace asking;

    private final CuboidLattice[] lattices;

    private final double[] frequencies;

    private final float[] amplitudes;

    private CuboidOctaves(CuboidAtlas atlas, CuboidLattice[] lattices, double[] frequencies, float[] amplitudes) {
        this.faces = atlas.faces();
        this.asking = CuboidAskingFace.of(atlas);
        this.lattices = lattices;
        this.frequencies = frequencies;
        this.amplitudes = amplitudes;
    }

    public static CuboidOctaves compile(CuboidAtlas atlas, double scale, BoxDivisors divisors,
            double @Nullable [] layerFactors, NoiseStack stack) {
        NoiseStack.Layer[] layers = stack.layers;
        CuboidLattice[] lattices = new CuboidLattice[layers.length];
        double[] frequencies = new double[layers.length];
        float[] amplitudes = new float[layers.length];
        for (int i = 0; i < layers.length; i++) {
            NoiseStack.Layer layer = layers[i];
            double frequency = layer.frequency();
            lattices[i] = lattice(atlas, CompiledOctaves.layerScale(scale, layerFactors, i, frequency), divisors,
                    layer.noise());
            frequencies[i] = frequency;
            amplitudes[i] = layer.amplitude();
        }

        return new CuboidOctaves(atlas, lattices, frequencies, amplitudes);
    }

    @SuppressWarnings("deprecation")
    private static CuboidLattice lattice(CuboidAtlas atlas, double layerScale, BoxDivisors divisors, Noise leaf) {
        if (leaf.getClass() == PerlinNoise.class) {
            PerlinNoise perlin = (PerlinNoise) leaf;
            return CuboidLattice.of(perlin.perms, perlin.offsetY, PerlinMath.NO_FUDGE, atlas, layerScale, divisors);
        }

        if (leaf.getClass() == SmearedPerlinNoise.class) {
            SmearedPerlinNoise smeared = (SmearedPerlinNoise) leaf;
            return CuboidLattice.of(smeared.perms, smeared.offsetY, smeared.fudgeYScale, atlas, layerScale,
                    divisors);
        }

        throw new IllegalArgumentException(NOT_PERLIN_LEAF + leaf.getClass().getName());
    }

    @Override
    public float sample(double x, double y, double z) {
        return sampleFlat(Part.VALUE, x, y, z);
    }

    public float sampleSecond(double x, double y, double z) {
        return sampleFlat(Part.SECOND_VALUE, x, y, z);
    }

    public float sampleOnFace(int atlasFace, double localX, double y, double localZ) {
        return sample(Part.VALUE, atlasFace, localX, y, localZ);
    }

    float warpX(int atlasFace, double localX, double localZ) {
        return sample(Part.WARP_X, atlasFace, localX, WARP_Y, localZ);
    }

    float warpZ(int atlasFace, double localX, double localZ) {
        return sample(Part.WARP_Z, atlasFace, localX, WARP_Y, localZ);
    }

    private float sampleFlat(Part part, double x, double y, double z) {
        int blockX = Mth.floor(x);
        int blockZ = Mth.floor(z);
        int atlasFace = this.asking.faceAt(blockX, blockZ);
        if (atlasFace == CuboidAskingFace.NO_FACE) {
            Crossing crossing = this.asking.crossingOf(blockX, blockZ);
            return sample(part, crossing.face(), this.asking.localX(crossing, x, z), y,
                    this.asking.localZ(crossing, x, z));
        }

        CuboidFace face = this.faces.get(atlasFace);
        return sample(part, atlasFace, x - face.minBlockX(), y, z - face.minBlockZ());
    }

    private float sample(Part part, int atlasFace, double localX, double y, double localZ) {
        float value = 0.0F;
        for (int i = 0; i < this.lattices.length; i++) {
            double octaveY = y * this.frequencies[i];
            value += this.amplitudes[i] * CuboidNoiseSampler.sample(part, this.lattices[i], atlasFace, localX,
                    GradientNoise.wrap(octaveY), localZ, octaveY);
        }

        return value;
    }

    @Override
    public boolean isIndexable() {
        return true;
    }
}
