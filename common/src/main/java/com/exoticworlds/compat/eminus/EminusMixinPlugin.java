package com.exoticworlds.compat.eminus;

import java.util.List;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.gate.ModPresence;
import com.exoticworlds.compat.gate.ModPresenceGatePlugin;

public class EminusMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String EMINUS_PACKAGE = "com/eminus/";

    private static final String TREE_CHANGES_CLASS = "com/eminus/render/tree/TreeChanges.class";
    private static final String PIPELINE_SPEC_MIXIN = "PipelineSpecMixin";
    private static final String PIPELINE_SPEC = "com/eminus/gpu/pipeline/PipelineSpec";
    private static final String BUILDER = "builder";
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
    private static final String COLUMN_COVERAGE = "com/eminus/cell/ColumnCoverage";
    private static final String HOLDS = "holds";
    private static final String HOLDS_DESCRIPTOR = "(II)Z";
    private static final String HOLDS_MIXIN = "ColumnCoverageHoldsMixin";
    private static final String SAVE_IMPORT_CLASS = "com/eminus/ingest/saved/SaveImport.class";
    private static final List<String> SAVE_IMPORT_MIXINS = List.of("SaveImportMixin", "SaveImportTileRunMixin",
            "SavedChunkMixin", "SavedColumnMixin", "ServerSaveMixin");

    private static final ModPresence EMINUS = withholdingSaveImports(ModPresence.gate(LOGGER,
            "[ew/eminus-compat] gate eminus_present"))
            .probing("com/eminus/cell/cache/CellCache.class")
            .checking("exotic_worlds.compat.eminus.mixins.json")
            .modPackages(EMINUS_PACKAGE)
            .withholding(TREE_MANAGER_CHANGES_MIXIN, EminusMixinPlugin::areChangesInTreeChanges)
            .withholding(TREE_CHANGES_MIXIN, () -> !areChangesInTreeChanges())
            .withholding(TREE_NODE_ACCESSOR, () -> !areChangesInTreeChanges())
            .withholding(MESH_FAILURES_MIXIN, () -> !areChangesInTreeChanges())
            .withholding(MISSING_COLUMNS_MIXIN, () -> !areChangesInTreeChanges())
            .withholding(WAITING_MIXIN, () -> !areChangesInTreeChanges())
            .withholding(PIPELINE_SPEC_MIXIN, () -> !canBuildPipelineSpecs())
            .withholding(REQUEST_MIXIN, EminusMixinPlugin::isFillCarriedByRequest)
            .withholding(FILL_REQUEST_MIXIN, () -> !isFillCarriedByRequest())
            .withholding(BEAM_SET_MIXIN, () -> !canDrawBeams())
            .withholding(BEAM_PASS_MIXIN, () -> !canDrawBeams())
            .withholding(RUNTIME_BEAMS_MIXIN, () -> !canDrawBeams())
            .withholding(HOLDS_MIXIN, () -> !hasCoverageHolds())
            .build();

    public EminusMixinPlugin() {
        super(EMINUS);
    }

    private static boolean areChangesInTreeChanges() {
        return ModPresence.hasResource(TREE_CHANGES_CLASS);
    }

    private static boolean canDrawBeams() {
        return ModPresence.hasResource(BEAM_PASS_CLASS);
    }

    private static ModPresence.Builder withholdingSaveImports(ModPresence.Builder gate) {
        for (String mixin : SAVE_IMPORT_MIXINS) {
            gate.withholding(mixin, () -> !canImportSaves());
        }

        return gate;
    }

    private static boolean canBuildPipelineSpecs() {
        return ModPresence.hasMethod(PIPELINE_SPEC, BUILDER, EminusInjectionTargets.PIPELINE_SPEC_BUILDER);
    }

    private static boolean canImportSaves() {
        return ModPresence.hasResource(SAVE_IMPORT_CLASS);
    }

    private static boolean hasCoverageHolds() {
        return ModPresence.hasMethod(COLUMN_COVERAGE, HOLDS, HOLDS_DESCRIPTOR);
    }

    private static boolean isFillCarriedByRequest() {
        return ModPresence.hasMethod(MESH_SERVICE, REQUEST, EminusInjectionTargets.MESH_SERVICE_FILL_REQUEST);
    }
}
