package com.exoticworlds.compat.eminus;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.gate.ModPresence;
import com.exoticworlds.compat.gate.ModPresenceGatePlugin;

public class EminusMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String EMINUS_PACKAGE = "com/eminus/";

    private static final String TREE_CHANGES_CLASS = "com/eminus/render/tree/TreeChanges.class";
    private static final String NEAR_BOX_CLASS = "com/eminus/handoff/NearBox.class";
    private static final String FAR_QUADS_MIXIN = "FarQuadsMixin";
    private static final String FAR_QUADS_EVERY_PIPELINE_MIXIN = "FarQuadsEveryPipelineMixin";
    private static final String TREE_MANAGER_CHANGES_MIXIN = "TreeManagerChangesMixin";
    private static final String TREE_CHANGES_MIXIN = "TreeChangesMixin";
    private static final String TREE_NODE_ACCESSOR = "TreeNodeAccessor";
    private static final String MESH_FAILURES_MIXIN = "MeshServiceFailuresMixin";
    private static final String MISSING_COLUMNS_MIXIN = "ColumnCoverageMissingMixin";
    private static final String WAITING_MIXIN = "TreeTraversalWaitingMixin";
    private static final String MESH_SERVICE = "com/eminus/mesh/MeshService";
    private static final String REQUEST = "request";
    private static final String REQUEST_MIXIN = "MeshServiceRequestMixin";
    private static final String FILL_REQUEST_MIXIN = "MeshServiceFillRequestMixin";
    private static final String BEAM_PASS_CLASS = "com/eminus/client/render/beam/BeamPass.class";
    private static final String BEAM_SET_MIXIN = "BeamSetMixin";
    private static final String BEAM_PASS_MIXIN = "BeamPassMixin";
    private static final String RUNTIME_BEAMS_MIXIN = "DimensionRuntimeBeamsMixin";

    private static final ModPresence EMINUS = ModPresence.gate(LOGGER, "[ew/eminus-compat] gate eminus_present")
            .probing("com/eminus/cell/cache/CellCache.class")
            .checking("exotic_worlds.compat.eminus.mixins.json")
            .modPackages(EMINUS_PACKAGE)
            .withholding(TREE_MANAGER_CHANGES_MIXIN, EminusMixinPlugin::changesLiveInTreeChanges)
            .withholding(TREE_CHANGES_MIXIN, () -> !changesLiveInTreeChanges())
            .withholding(TREE_NODE_ACCESSOR, () -> !changesLiveInTreeChanges())
            .withholding(MESH_FAILURES_MIXIN, () -> !changesLiveInTreeChanges())
            .withholding(MISSING_COLUMNS_MIXIN, () -> !changesLiveInTreeChanges())
            .withholding(WAITING_MIXIN, () -> !changesLiveInTreeChanges())
            .withholding(FAR_QUADS_MIXIN, EminusMixinPlugin::solidPipelineSplit)
            .withholding(FAR_QUADS_EVERY_PIPELINE_MIXIN, () -> !solidPipelineSplit())
            .withholding(REQUEST_MIXIN, EminusMixinPlugin::requestCarriesFill)
            .withholding(FILL_REQUEST_MIXIN, () -> !requestCarriesFill())
            .withholding(BEAM_SET_MIXIN, () -> !drawsBeams())
            .withholding(BEAM_PASS_MIXIN, () -> !drawsBeams())
            .withholding(RUNTIME_BEAMS_MIXIN, () -> !drawsBeams())
            .build();

    public EminusMixinPlugin() {
        super(EMINUS);
    }

    private static boolean changesLiveInTreeChanges() {
        return ModPresence.probe(TREE_CHANGES_CLASS);
    }

    private static boolean solidPipelineSplit() {
        return ModPresence.probe(NEAR_BOX_CLASS);
    }

    private static boolean drawsBeams() {
        return ModPresence.probe(BEAM_PASS_CLASS);
    }

    private static boolean requestCarriesFill() {
        return ModPresence.probeMethod(MESH_SERVICE, REQUEST, EminusInjectionTargets.MESH_SERVICE_FILL_REQUEST);
    }
}
