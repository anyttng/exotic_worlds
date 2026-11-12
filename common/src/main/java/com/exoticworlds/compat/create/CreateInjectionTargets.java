package com.exoticworlds.compat.create;

public final class CreateInjectionTargets {
    public static final String TRACK_EDGE_GET_POSITION =
            "Lcom/zurrtum/create/content/trains/graph/TrackEdge;getPosition"
                    + "(Lcom/zurrtum/create/content/trains/graph/TrackGraph;D)Lnet/minecraft/world/phys/Vec3;";

    public static final String TRACK_NODE_LOCATION_GET_LOCATION =
            "Lcom/zurrtum/create/content/trains/graph/TrackNodeLocation;getLocation()Lnet/minecraft/world/phys/Vec3;";

    private CreateInjectionTargets() {
    }
}
