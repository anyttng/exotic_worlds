package com.exoticworlds.compat.electroenergetics;

public final class ElectroEnergeticsInjectionTargets {
    public static final String WIRE_POS_AT =
            "Lcom/george_vi/electroenergetics/foundation/QuadraticWireHelper;posAt"
                    + "(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;FF)Lnet/minecraft/world/phys/Vec3;";

    public static final String WIRE_POS_AT_UNIT_DIP =
            "Lcom/george_vi/electroenergetics/foundation/QuadraticWireHelper;posAt"
                    + "(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;";

    public static final String CONNECTION_POINT_POS_AT =
            "Lcom/george_vi/electroenergetics/foundation/nodes/NodeConnectionPoint;posAt"
                    + "(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;";

    public static final String NODE_GET_POSITION =
            "Lcom/george_vi/electroenergetics/foundation/nodes/InWorldNode;getPosition"
                    + "(Lnet/minecraft/world/level/Level;)Lnet/minecraft/world/phys/Vec3;";

    public static final String ALL_WIRE_CONNECTIONS =
            "Lcom/george_vi/electroenergetics/client/WireRenderer;getAllConnections()Ljava/util/List;";

    public static final String CATENARY_LINES = "Lcom/george_vi/electroenergetics/client/WireRenderer;CATENARY:Ljava/util/List;";

    private ElectroEnergeticsInjectionTargets() {
    }
}
