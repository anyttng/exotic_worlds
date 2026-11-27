package com.exoticworlds.engine.noise.cuboid;

import java.util.List;

import com.exoticworlds.core.fold.cuboid.CuboidAtlas;
import com.exoticworlds.core.fold.cuboid.CuboidFace;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.engine.noise.PerlinMath;
import com.exoticworlds.engine.noise.SimplexField;
import com.exoticworlds.engine.noise.cuboid.CuboidCrossings.Crossing;
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

    private final CuboidAskingFace asking;

    private final CuboidLattice lattice;

    private CuboidSimplexStandIn(WorldFold fold, double scale, CuboidAtlas atlas, CuboidLattice lattice) {
        this.fold = fold;
        this.scale = scale;
        this.faces = atlas.faces();
        this.asking = CuboidAskingFace.of(atlas);
        this.lattice = lattice;
    }

    public static CuboidSimplexStandIn of(WorldFold fold, CuboidAtlas atlas, double scale, SimplexNoise noise) {
        return new CuboidSimplexStandIn(fold, scale, atlas,
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
    public boolean canCarry() {
        return true;
    }

    @Override
    public double sample(SimplexNoise noise, double x, double z) {
        int blockX = Mth.floor(x);
        int blockZ = Mth.floor(z);
        int atlasFace = this.asking.faceAt(blockX, blockZ);
        if (atlasFace == CuboidAskingFace.NO_FACE) {
            Crossing crossing = this.asking.crossingOf(blockX, blockZ);
            return sampleOnFace(crossing.face(), this.asking.localX(crossing, x, z),
                    this.asking.localZ(crossing, x, z));
        }

        CuboidFace face = this.faces.get(atlasFace);
        return sampleOnFace(atlasFace, x - face.minBlockX(), z - face.minBlockZ());
    }

    double sampleOnFace(int atlasFace, double localX, double localZ) {
        return AMPLITUDE * CuboidNoiseSampler.sample(Part.VALUE, this.lattice, atlasFace, localX, PLANE, localZ,
                PLANE);
    }
}
