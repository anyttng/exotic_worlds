package com.exoticworlds.compat.eminus;

public final class EminusInjectionTargets {
    public static final String NODE_TABLE_GET = "Lcom/eminus/render/tree/NodeTable;get(J)Lcom/eminus/render/tree/TreeNode;";
    public static final String COVERAGE_PACK = "Lcom/eminus/cell/ColumnCoverage;pack(II)J";
    public static final String MESH_QUEUE_POLL = "Lcom/eminus/mesh/MeshQueue;poll()Lcom/eminus/mesh/MeshTask;";
    public static final String MESH_LISTENER_MESHED = "Lcom/eminus/mesh/MeshListener;meshed(Lcom/eminus/mesh/CellMesh;J)V";
    public static final String MESH_LISTENER_FAILED = "Lcom/eminus/mesh/MeshListener;failed(JJ)V";

    private EminusInjectionTargets() {
    }
}
