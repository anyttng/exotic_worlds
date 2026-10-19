package com.exoticworlds.compat.eminus;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.eminus.client.session.ClientSession;
import com.eminus.gpu.pipeline.PipelineSpec;
import com.mojang.logging.LogUtils;

public final class LapClip {
    public static final String DEFINE = "TOROIDAL_LAP";
    public static final String DEFINE_X = "TOROIDAL_LAP_X";
    public static final String DEFINE_Z = "TOROIDAL_LAP_Z";
    public static final String DEFINE_SKEW = "TOROIDAL_LAP_SKEW";

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String NAMESPACE = "eminus";
    private static final String NEWLINE = "\n";
    private static final String SHADER_FOLDER = "shaders/";
    private static final String FRAGMENT_EXTENSION = ".fsh";

    private record Entry(String marker, String origin, String voxel, String variant) {
    }

    private static final Map<String, Entry> ENTRIES = Map.of(
            "core/far_quads", new Entry("void main() {", "cellOrigin", "voxelPoint", "variantInfo"),
            "core/far_pack", new Entry("EminusFragment eminus_fragment() {", "eminus_cellOrigin",
                    "eminus_voxelPoint", "eminus_variantInfo"));

    private record Frame(String sampler, String function) {
    }

    private static final List<Frame> FRAMES = List.of(
            new Frame("uniform sampler2D EminusAtlas;", function("EminusCameraBlockPos", "EminusCameraOffset")),
            new Frame("uniform sampler2D Atlas;", function("CameraBlockPos", "CameraOffset")));

    private LapClip() {
    }

    private static String function(String cameraBlockPos, String cameraOffset) {
        return String.join(NEWLINE,
                "#ifdef " + DEFINE,
                "bool toroidal_outside_lap(ivec3 origin, vec3 voxel, int level) {",
                "    vec2 point = vec2(origin.xz - " + cameraBlockPos + ".xz) + voxel.xz * float(1 << level) + "
                        + cameraOffset + ".xz;",
                "    float shear = 0.0;",
                "#if " + DEFINE_Z + " > 0",
                "    if (abs(point.y) >= float(" + DEFINE_Z + ") * 0.5) {",
                "        return true;",
                "    }",
                "    shear = point.y / float(" + DEFINE_Z + ") * float(" + DEFINE_SKEW + ");",
                "#endif",
                "#if " + DEFINE_X + " > 0",
                "    if (abs(point.x - shear) >= float(" + DEFINE_X + ") * 0.5) {",
                "        return true;",
                "    }",
                "#endif",
                "    return false;",
                "}",
                "#endif",
                "");
    }

    public static PipelineSpec.Builder clipped(PipelineSpec.Builder builder) {
        CellFold fold = CellFoldHolder.foldOf(ClientSession.runtime());
        if (!fold.repeats()) {
            return builder;
        }

        return builder.withDefine(DEFINE)
                .withDefine(DEFINE_X, fold.lapXBlocks())
                .withDefine(DEFINE_Z, fold.lapZBlocks())
                .withDefine(DEFINE_SKEW, fold.skewBlocks());
    }

    public static @Nullable String fragmentShader(String resourcePath) {
        return resourcePath.startsWith(SHADER_FOLDER) && resourcePath.endsWith(FRAGMENT_EXTENSION)
                ? resourcePath.substring(SHADER_FOLDER.length(), resourcePath.length() - FRAGMENT_EXTENSION.length())
                : null;
    }

    public static @Nullable String rewrite(String namespace, @Nullable String path, @Nullable String source) {
        if (source == null || path == null || !NAMESPACE.equals(namespace)) {
            return source;
        }

        Entry entry = ENTRIES.get(path);
        if (entry == null) {
            return source;
        }

        int marker = source.indexOf(entry.marker());
        if (marker < 0) {
            LOGGER.warn("[ew/eminus-compat] lap_clip_skipped shader={} reason=no_entry_point", path);
            return source;
        }

        Frame frame = frameOf(source);
        if (frame == null) {
            LOGGER.warn("[ew/eminus-compat] lap_clip_skipped shader={} reason=unknown_frame", path);
            return source;
        }

        int body = marker + entry.marker().length();
        String discard = String.join(NEWLINE, "",
                "#ifdef " + DEFINE,
                "    if (toroidal_outside_lap(" + entry.origin() + ", " + entry.voxel() + ", " + entry.variant()
                        + ".w)) {",
                "        discard;",
                "    }",
                "#endif");
        return source.substring(0, marker) + frame.function() + source.substring(marker, body) + discard
                + source.substring(body);
    }

    private static @Nullable Frame frameOf(String source) {
        for (Frame frame : FRAMES) {
            if (source.contains(frame.sampler())) {
                return frame;
            }
        }

        return null;
    }
}
