package com.exoticworlds.engine.noise.cuboid;

import java.util.List;

import com.exoticworlds.core.CuboidAtlas;
import com.exoticworlds.core.CuboidFace;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.PerlinMath;
import com.exoticworlds.engine.noise.SimplexField;
import com.exoticworlds.engine.noise.cuboid.CuboidNoiseSampler.Part;

import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

public final class CuboidSimplexStandIn implements SimplexField {
    static final double AMPLITUDE = 1.492;

    static final double RATE = 1.939;

    private static final double BETWEEN_PLANES = 0.5;

    private static final double PLANE = 0.0;

    private final WorldFold fold;

    private final double scale;

    private final List<CuboidFace> faces;

    private final CuboidLattice lattice;

    private CuboidSimplexStandIn(WorldFold fold, double scale, List<CuboidFace> faces, CuboidLattice lattice) {
        this.fold = fold;
        this.scale = scale;
        this.faces = faces;
        this.lattice = lattice;
    }

    public static CuboidSimplexStandIn of(WorldFold fold, CuboidAtlas atlas, double scale, SimplexNoise noise) {
        return new CuboidSimplexStandIn(fold, scale, atlas.faces(),
                CuboidLattice.of(noise.perms, BETWEEN_PLANES, PerlinMath.NO_FUDGE, atlas, scale * RATE));
    }

    @Override
    public WorldFold fold() {
        return this.fold;
    }

    @Override
    public double scale() {
        return this.scale;
    }

    @Override
    public boolean carries() {
        return true;
    }

    @Override
    public double sample(SimplexNoise noise, double x, double z) {
        int atlasFace = CuboidOctaves.faceAt(this.faces, Mth.floor(x), Mth.floor(z));
        CuboidFace face = this.faces.get(atlasFace);
        return sampleOnFace(atlasFace, x - face.minBlockX(), z - face.minBlockZ());
    }

    double sampleOnFace(int atlasFace, double localX, double localZ) {
        return AMPLITUDE * CuboidNoiseSampler.sample(Part.VALUE, this.lattice, atlasFace, localX, PLANE, localZ,
                PLANE);
    }
}
