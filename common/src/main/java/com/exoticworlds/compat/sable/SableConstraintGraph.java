package com.exoticworlds.compat.sable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;

public final class SableConstraintGraph {
    private final List<SableConstraintEdge> edges = new ArrayList<>();
    private @Nullable IdentityHashMap<PhysicsPipelineBody, List<PhysicsPipelineBody>> groups;

    public static List<PhysicsPipelineBody> groupOf(PhysicsPipeline pipeline, PhysicsPipelineBody start) {
        return pipeline instanceof SableConstraintGraphHolder holder
                ? holder.exotic_worlds$constraintGraph().groupOf(start)
                : List.of(start);
    }

    public static boolean isAlone(PhysicsPipeline pipeline, PhysicsPipelineBody body) {
        return !(pipeline instanceof SableConstraintGraphHolder holder) || holder.exotic_worlds$constraintGraph().isAlone(body);
    }

    public SableConstraintEdge record(PhysicsPipelineBody first, PhysicsPipelineBody second) {
        SableConstraintEdge edge = new SableConstraintEdge(this, first, second);
        this.edges.add(edge);
        this.groups = null;
        return edge;
    }

    public void drop(SableConstraintEdge edge) {
        if (this.edges.removeIf(candidate -> candidate == edge)) {
            this.groups = null;
        }
    }

    public void dropBody(PhysicsPipelineBody body) {
        if (this.edges.removeIf(edge -> edge.touches(body))) {
            this.groups = null;
        }
    }

    public int size() {
        return this.edges.size();
    }

    public List<PhysicsPipelineBody> groupOf(PhysicsPipelineBody start) {
        if (this.edges.isEmpty()) {
            return List.of(start);
        }

        List<PhysicsPipelineBody> group = this.groups().get(start);
        return group == null ? List.of(start) : live(group);
    }

    public boolean isAlone(PhysicsPipelineBody body) {
        return this.edges.isEmpty() || this.groups().get(body) == null;
    }

    private IdentityHashMap<PhysicsPipelineBody, List<PhysicsPipelineBody>> groups() {
        IdentityHashMap<PhysicsPipelineBody, List<PhysicsPipelineBody>> groups = this.groups;
        if (groups == null) {
            groups = this.walkGroups();
            this.groups = groups;
        }

        return groups;
    }

    private static List<PhysicsPipelineBody> live(List<PhysicsPipelineBody> group) {
        for (PhysicsPipelineBody member : group) {
            if (member.isRemoved()) {
                return group.stream().filter(body -> !body.isRemoved()).toList();
            }
        }

        return group;
    }

    private IdentityHashMap<PhysicsPipelineBody, List<PhysicsPipelineBody>> walkGroups() {
        IdentityHashMap<PhysicsPipelineBody, List<PhysicsPipelineBody>> neighbors = new IdentityHashMap<>();
        for (SableConstraintEdge edge : this.edges) {
            neighbors.computeIfAbsent(edge.first(), body -> new ArrayList<>()).add(edge.second());
            neighbors.computeIfAbsent(edge.second(), body -> new ArrayList<>()).add(edge.first());
        }

        IdentityHashMap<PhysicsPipelineBody, List<PhysicsPipelineBody>> groups = new IdentityHashMap<>();
        Set<PhysicsPipelineBody> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (PhysicsPipelineBody start : neighbors.keySet()) {
            if (!seen.add(start)) {
                continue;
            }

            List<PhysicsPipelineBody> group = new ArrayList<>();
            group.add(start);
            for (int index = 0; index < group.size(); index++) {
                for (PhysicsPipelineBody other : Objects.requireNonNull(neighbors.get(group.get(index)),
                        "every edge end is a key")) {
                    if (seen.add(other)) {
                        group.add(other);
                    }
                }
            }

            List<PhysicsPipelineBody> members = List.copyOf(group);
            for (PhysicsPipelineBody member : members) {
                groups.put(member, members);
            }
        }

        return groups;
    }
}
