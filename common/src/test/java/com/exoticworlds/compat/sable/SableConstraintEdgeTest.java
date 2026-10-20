package com.exoticworlds.compat.sable;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.api.physics.mass.MassData;

class SableConstraintEdgeTest {
    private static final class IndistinguishableBody implements PhysicsPipelineBody {
        @Override
        public int getRuntimeId() {
            return NULL_RUNTIME_ID;
        }

        @Override
        public MassData getMassTracker() {
            return null;
        }

        @Override
        public boolean isRemoved() {
            return false;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof IndistinguishableBody;
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }

    @Test
    void touchesEitherEndAndNoStranger() {
        SableConstraintGraph graph = new SableConstraintGraph();
        PhysicsPipelineBody first = new IndistinguishableBody();
        PhysicsPipelineBody second = new IndistinguishableBody();
        SableConstraintEdge edge = graph.record(first, second);

        assertTrue(edge.touches(first));
        assertTrue(edge.touches(second));
        assertFalse(edge.touches(new IndistinguishableBody()));
    }
}
