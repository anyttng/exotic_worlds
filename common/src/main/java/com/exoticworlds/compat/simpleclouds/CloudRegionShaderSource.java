package com.exoticworlds.compat.simpleclouds;

import java.util.Arrays;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.ExoticWorlds;

public final class CloudRegionShaderSource {
    public static final String SHADER_ID = "simpleclouds:cloud_regions";
    public static final int REGION_BYTES = 32;
    public static final int FOLDED_REGION_BYTES = 64;
    public static final int REGION_BUFFER_BYTES = 320;
    public static final int FOLDED_REGION_BUFFER_BYTES = 640;

    private static final int REGION_FLOATS = 8;
    private static final int TRANSFORM_00 = 4;
    private static final int TRANSFORM_01 = 5;
    private static final int TRANSFORM_10 = 6;
    private static final int TRANSFORM_11 = 7;

    private static final String STRUCT_ANCHOR = "mat2 transform;";
    private static final String STRUCT_FIELDS = "mat2 transform;\n    vec2 lapA;\n    vec2 lapB;\n    mat2 lapInverse;";
    private static final String CIRCLE_ANCHOR = "vec3 circle(CloudRegion region, vec2 coord)";
    private static final String CALL_ANCHOR = "circle(cloudRegions.data[i], coord)";
    private static final String SEATED_CALL = "circle(cloudRegions.data[i], toroidal_seat(cloudRegions.data[i], coord))";
    private static final String SEAT_FUNCTION = """
            vec2 toroidal_seat(CloudRegion region, vec2 coord)
            {
                vec2 p = vec2(region.posX, region.posZ);
                vec2 base = -round(region.lapInverse * (region.transform * (coord - p)));
                vec2 best = coord;
                float bestLength = -1.0;
                for (int i = -1; i <= 1; i++)
                {
                    for (int j = -1; j <= 1; j++)
                    {
                        vec2 k = base + vec2(float(i), float(j));
                        vec2 seated = coord + k.x * region.lapA + k.y * region.lapB;
                        vec2 d = region.transform * (seated - p);
                        float l = dot(d, d);
                        if (bestLength < 0.0 || l < bestLength
                            || (l == bestLength && (seated.x < best.x || (seated.x == best.x && seated.y < best.y))))
                        {
                            best = seated;
                            bestLength = l;
                        }
                    }
                }
                return best;
            }

            """;

    private static volatile boolean rewritten;

    public static String rewrite(String source) {
        boolean anchored = count(source, STRUCT_ANCHOR) == 1 && count(source, CIRCLE_ANCHOR) == 1
                && count(source, CALL_ANCHOR) == 1;
        if (!anchored) {
            ExoticWorlds.LOGGER.warn("[sc-compat] cloud_regions_shader rewritten=false text=the shader no longer carries"
                    + " the anchors of Simple Clouds 0.7.3, so the drawn cloud field stays unfolded");
            return source;
        }

        rewritten = true;
        return source.replace(STRUCT_ANCHOR, STRUCT_FIELDS)
                .replace(CIRCLE_ANCHOR, SEAT_FUNCTION + CIRCLE_ANCHOR)
                .replace(CALL_ANCHOR, SEATED_CALL);
    }

    public static boolean rewritten() {
        return rewritten;
    }

    public static float[] withLattice(float[] region, @Nullable CloudShape shape) {
        if (!rewritten || region.length != REGION_FLOATS) {
            return region;
        }

        float[] folded = Arrays.copyOf(region, REGION_FLOATS + CloudLattice.PACKED_SIZE);
        CloudLattice lattice = shape == null
                ? CloudLattice.NONE
                : SimpleCloudsShapes.latticeOf(shape, region[TRANSFORM_00], region[TRANSFORM_01], region[TRANSFORM_10],
                        region[TRANSFORM_11]);
        lattice.pack(folded, REGION_FLOATS);
        return folded;
    }

    private static int count(String source, String anchor) {
        int found = 0;
        for (int at = source.indexOf(anchor); at >= 0; at = source.indexOf(anchor, at + anchor.length())) {
            found++;
        }

        return found;
    }

    private CloudRegionShaderSource() {
    }
}
