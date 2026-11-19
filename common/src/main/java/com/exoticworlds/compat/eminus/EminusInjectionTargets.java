package com.exoticworlds.compat.eminus;

public final class EminusInjectionTargets {
    public static final String NODE_TABLE_GET = "Lcom/eminus/render/tree/NodeTable;get(J)Lcom/eminus/render/tree/TreeNode;";
    public static final String COVERAGE_PACK = "Lcom/eminus/cell/ColumnCoverage;pack(II)J";
    public static final String MESH_QUEUE_POLL = "Lcom/eminus/mesh/MeshQueue;poll()Lcom/eminus/mesh/MeshTask;";
    public static final String MESH_LISTENER_MESHED = "Lcom/eminus/mesh/MeshListener;meshed(Lcom/eminus/mesh/CellMesh;J)V";
    public static final String MESH_LISTENER_FAILED = "Lcom/eminus/mesh/MeshListener;failed(JJ)V";
    public static final String MESH_SERVICE_FILL_REQUEST = "(JLcom/eminus/cell/cache/CellHandle;IJFZ)V";
    public static final String FAR_PIPELINE_BUILDER = "(Lcom/eminus/gpu/Location;Lcom/eminus/gpu/Location;"
            + "Lcom/eminus/gpu/Capabilities;Lcom/eminus/model/port/VariantDraw;Z)"
            + "Lcom/eminus/gpu/pipeline/PipelineSpec$Builder;";
    public static final String FAR_PIPELINE_BUILDER_WITH_CUTOUT = "(Lcom/eminus/gpu/Location;Lcom/eminus/gpu/Location;F"
            + "Lcom/eminus/gpu/Capabilities;Lcom/eminus/model/port/VariantDraw;Z)"
            + "Lcom/eminus/gpu/pipeline/PipelineSpec$Builder;";

    private EminusInjectionTargets() {
    }
}
