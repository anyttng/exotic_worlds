package com.exoticworlds.compat.eminus;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EminusMixinPluginTest {
    private static final String PIPELINE_SPEC = "com.eminus.gpu.pipeline.PipelineSpec";
    private static final String LAP_CLIP_MIXIN = "com.exoticworlds.compat.eminus.mixin.PipelineSpecMixin";

    @Test
    void theLapClipAppliesOnTheEminusBuildTheSuiteCarries() {
        assertTrue(new EminusMixinPlugin().shouldApplyMixin(PIPELINE_SPEC, LAP_CLIP_MIXIN),
                "the gate keeps PipelineSpecMixin off this Eminus build, so no far pipeline takes the lap defines");
    }
}
