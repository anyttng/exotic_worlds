package com.exoticworlds.engine.noise;

import java.util.List;

import com.exoticworlds.core.CuboidAtlas;
import com.exoticworlds.core.CuboidFace;

import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.synth.GradientNoise;

final class CuboidOctaves {
    private final List<CuboidFace> faces;

    private final CuboidLattice[] lattices;

    private final double[] frequencies;

    private final float[] amplitudes;

    CuboidOctaves(CuboidAtlas atlas, CuboidLattice[] lattices, double[] frequencies, float[] amplitudes) {
        this.faces = atlas.faces();
        this.lattices = lattices;
        this.frequencies = frequencies;
        this.amplitudes = amplitudes;
    }

    float sample(double x, double y, double z) {
        int atlasFace = faceAt(Mth.floor(x), Mth.floor(z));
        CuboidFace face = this.faces.get(atlasFace);
        double localX = x - face.minBlockX();
        double localZ = z - face.minBlockZ();
        float value = 0.0F;
        for (int i = 0; i < this.lattices.length; i++) {
            double octaveY = y * this.frequencies[i];
            value += this.amplitudes[i] * CuboidNoiseSampler.sample(this.lattices[i], atlasFace, localX,
                    GradientNoise.wrap(octaveY), localZ, octaveY);
        }

        return value;
    }

    private int faceAt(int blockX, int blockZ) {
        for (int i = 0; i < this.faces.size(); i++) {
            if (this.faces.get(i).contains(blockX, blockZ)) {
                return i;
            }
        }

        throw new IllegalArgumentException("A cuboid noise samples inside a face, got x=" + blockX + " z=" + blockZ);
    }
}
