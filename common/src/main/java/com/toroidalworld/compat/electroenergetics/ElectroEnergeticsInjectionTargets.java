package com.toroidalworld.compat.electroenergetics;

public final class ElectroEnergeticsInjectionTargets {
    public static final String WIRE_POS_AT =
            "Lcom/george_vi/electroenergetics/foundation/QuadraticWireHelper;posAt"
                    + "(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;FF)Lnet/minecraft/world/phys/Vec3;";

    public static final String CONNECTION_POINT_POS_AT =
            "Lcom/george_vi/electroenergetics/foundation/nodes/NodeConnectionPoint;posAt"
                    + "(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;";

    public static final String NODE_GET_POSITION =
            "Lcom/george_vi/electroenergetics/foundation/nodes/InWorldNode;getPosition"
                    + "(Lnet/minecraft/world/level/Level;)Lnet/minecraft/world/phys/Vec3;";

    public static final String STRUCTURE_TEMPLATE_MIXIN = "com.george_vi.electroenergetics.mixins.StructureTemplateMixin";
    public static final String CAPTURE_HANDLER = "fillFromWorld";
    public static final String PLACE_HANDLER = "placeInWorld";

    public static final String SCHEMATIC_PRINTER_MIXIN = "com.george_vi.electroenergetics.mixins.SchematicPrinterMixin";
    public static final String PRINT_ADVANCE_HANDLER = "electroEnergetics$advanceCurrentPos";
    public static final String PRINT_TARGET_HANDLER = "electroEnergetics$handleCurrentTarget";

    private ElectroEnergeticsInjectionTargets() {
    }
}
