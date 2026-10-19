package com.exoticworlds.compat.eminus;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;

public class EminusMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String TREE_CHANGES_CLASS = "com/eminus/render/tree/TreeChanges.class";
    private static final String NEAR_BOX_CLASS = "com/eminus/handoff/NearBox.class";
    private static final String FAR_QUADS_MIXIN = "FarQuadsMixin";
    private static final String FAR_QUADS_EVERY_PIPELINE_MIXIN = "FarQuadsEveryPipelineMixin";
    private static final String TREE_MANAGER_CHANGES_MIXIN = "TreeManagerChangesMixin";
    private static final String TREE_CHANGES_MIXIN = "TreeChangesMixin";
    private static final String MESH_FAILURES_MIXIN = "MeshServiceFailuresMixin";
    private static final String MISSING_COLUMNS_MIXIN = "ColumnCoverageMissingMixin";
    private static final String WAITING_MIXIN = "TreeTraversalWaitingMixin";

    private static final ModPresence EMINUS = ModPresence.gate(LOGGER, "[ew/eminus-compat] gate eminus_present")
            .probing("com/eminus/cell/cache/CellCache.class")
            .checking("exotic_worlds.compat.eminus.mixins.json")
            .withholding(TREE_MANAGER_CHANGES_MIXIN, EminusMixinPlugin::changesLiveInTreeChanges)
            .withholding(TREE_CHANGES_MIXIN, () -> !changesLiveInTreeChanges())
            .withholding(MESH_FAILURES_MIXIN, () -> !changesLiveInTreeChanges())
            .withholding(MISSING_COLUMNS_MIXIN, () -> !changesLiveInTreeChanges())
            .withholding(WAITING_MIXIN, () -> !changesLiveInTreeChanges())
            .withholding(FAR_QUADS_MIXIN, EminusMixinPlugin::solidPipelineSplit)
            .withholding(FAR_QUADS_EVERY_PIPELINE_MIXIN, () -> !solidPipelineSplit())
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
}
