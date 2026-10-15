package com.exoticworlds.compat.eminus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LapClipTest {
    private static final String QUADS = """
            #version 330
            uniform sampler2D Atlas;
            void main() {
                fragColor = vec4(1.0);
            }
            """;
    private static final String PREFIXED_QUADS = """
            #version 330
            uniform sampler2D EminusAtlas;
            void main() {
                fragColor = vec4(1.0);
            }
            """;
    private static final String UNKNOWN_QUADS = """
            #version 330
            uniform sampler2D Albedo;
            void main() {
                fragColor = vec4(1.0);
            }
            """;
    private static final String PACK = """
            uniform sampler2D Atlas;
            EminusFragment eminus_fragment() {
                return f;
            }
            """;

    @Test
    void theFarFragmentDiscardsOutsideTheLapFirst() {
        String rewritten = LapClip.rewrite("eminus", "core/far_quads", QUADS);
        int function = rewritten.indexOf("bool toroidal_outside_lap(");
        int main = rewritten.indexOf("void main() {");
        int discard = rewritten.indexOf("toroidal_outside_lap(cellOrigin, voxelPoint, variantInfo.w)");
        int body = rewritten.indexOf("fragColor = vec4(1.0);");
        assertTrue(function >= 0 && function < main && main < discard && discard < body, rewritten);
    }

    @Test
    void thePackFragmentDiscardsWithItsOwnInputs() {
        String rewritten = LapClip.rewrite("eminus", "core/far_pack", PACK);
        assertTrue(rewritten.contains("toroidal_outside_lap(eminus_cellOrigin, eminus_voxelPoint,"
                + " eminus_variantInfo.w)"), rewritten);
    }

    @Test
    void aPrefixedFrameIsReadByItsPrefixedCamera() {
        String rewritten = LapClip.rewrite("eminus", "core/far_quads", PREFIXED_QUADS);
        assertTrue(rewritten.contains("origin.xz - EminusCameraBlockPos.xz"), rewritten);
        assertTrue(rewritten.contains("+ EminusCameraOffset.xz"), rewritten);
    }

    @Test
    void thePublishedFrameIsReadByItsOwnCamera() {
        String rewritten = LapClip.rewrite("eminus", "core/far_quads", QUADS);
        assertTrue(rewritten.contains("origin.xz - CameraBlockPos.xz"), rewritten);
        assertTrue(rewritten.contains("+ CameraOffset.xz"), rewritten);
        assertFalse(rewritten.contains("EminusCamera"), rewritten);
    }

    @Test
    void aFrameOfNeitherNameIsLeftAlone() {
        assertEquals(UNKNOWN_QUADS, LapClip.rewrite("eminus", "core/far_quads", UNKNOWN_QUADS));
    }

    @Test
    void anyOtherShaderIsLeftAlone() {
        assertEquals(QUADS, LapClip.rewrite("eminus", "core/far_boxes", QUADS));
        assertEquals(QUADS, LapClip.rewrite("minecraft", "core/far_quads", QUADS));
        assertEquals(QUADS, LapClip.rewrite("eminus", null, QUADS));
        assertEquals("no entry point", LapClip.rewrite("eminus", "core/far_quads", "no entry point"));
    }

    @Test
    void onlyAFragmentFileNamesAShader() {
        assertEquals("core/far_quads", LapClip.fragmentShader("shaders/core/far_quads.fsh"));
        assertNull(LapClip.fragmentShader("shaders/core/far_quads.vsh"));
        assertNull(LapClip.fragmentShader("shaders/include/far_frame.glsl"));
    }
}
