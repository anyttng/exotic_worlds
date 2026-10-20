package com.exoticworlds.compat.sable;

import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;

public record SableConstraintEdge(SableConstraintGraph graph, PhysicsPipelineBody first, PhysicsPipelineBody second) {
    public void drop() {
        this.graph.drop(this);
    }

    boolean touches(PhysicsPipelineBody body) {
        return body == this.first || body == this.second;
    }
}
