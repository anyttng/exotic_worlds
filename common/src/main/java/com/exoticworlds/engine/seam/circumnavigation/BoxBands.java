package com.exoticworlds.engine.seam.circumnavigation;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import com.exoticworlds.core.fold.cuboid.CuboidAtlas;
import com.exoticworlds.core.fold.cuboid.CuboidFace;
import com.exoticworlds.core.fold.cuboid.CuboidFace.BoxVector;

public final class BoxBands {
    public enum Band {
        AROUND_A,
        AROUND_B,
        AROUND_C
    }

    private static final int BAND_SIDES = 2;

    private final CuboidAtlas atlas;

    private final Map<Band, Integer> lapBlocks;

    private BoxBands(CuboidAtlas atlas, Map<Band, Integer> lapBlocks) {
        this.atlas = atlas;
        this.lapBlocks = lapBlocks;
    }

    static BoxBands of(CuboidAtlas atlas) {
        Map<Band, Integer> edgeBlocks = new EnumMap<>(Band.class);
        for (CuboidFace face : atlas.faces()) {
            for (Band band : Band.values()) {
                if (componentOf(face.alongX(), band) != 0) {
                    edgeBlocks.put(band, face.widthBlocks());
                }

                if (componentOf(face.alongZ(), band) != 0) {
                    edgeBlocks.put(band, face.depthBlocks());
                }
            }
        }

        int perimeterBlocks = 0;
        for (int edge : edgeBlocks.values()) {
            perimeterBlocks += edge;
        }

        Map<Band, Integer> lapBlocks = new EnumMap<>(Band.class);
        for (Map.Entry<Band, Integer> edge : edgeBlocks.entrySet()) {
            lapBlocks.put(edge.getKey(), BAND_SIDES * (perimeterBlocks - edge.getValue()));
        }

        return new BoxBands(atlas, lapBlocks);
    }

    boolean isOf(CuboidAtlas atlas) {
        return this.atlas == atlas;
    }

    int getLapBlocks(Band band) {
        return Objects.requireNonNull(this.lapBlocks.get(band), band.name());
    }

    static boolean isCap(CuboidFace face, Band band) {
        return componentOf(face.alongX(), band) == 0 && componentOf(face.alongZ(), band) == 0;
    }

    static double progressAround(CuboidFace face, Band band, double stepX, double stepZ) {
        return stepX * componentOf(face.alongZ(), band) - stepZ * componentOf(face.alongX(), band);
    }

    private static int componentOf(BoxVector direction, Band band) {
        return switch (band) {
            case AROUND_A -> direction.a();
            case AROUND_B -> direction.b();
            case AROUND_C -> direction.c();
        };
    }
}
