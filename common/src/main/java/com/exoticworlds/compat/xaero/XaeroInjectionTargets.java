package com.exoticworlds.compat.xaero;

public final class XaeroInjectionTargets {
    public static final String MAP_PROCESSOR_GET_LEAF_MAP_REGION =
            "Lxaero/map/MapProcessor;getLeafMapRegion(IIIZ)Lxaero/map/region/MapRegion;";
    public static final String GUI_MAP_RENDER_TEXTURED_MODAL_RECT_WITH_LIGHTING_3 =
            "Lxaero/map/gui/GuiMap;renderTexturedModalRectWithLighting3(Lorg/joml/Matrix4f;FFFF"
                    + "Lcom/mojang/renderpearl/api/textures/GpuTextureView;Z"
                    + "Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRenderer;)V";
    public static final String MAP_RENDER_HELPER_RENDER_DYNAMIC_HIGHLIGHT =
            "Lxaero/map/graphics/MapRenderHelper;renderDynamicHighlight(Lcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lcom/mojang/blaze3d/vertex/VertexConsumer;IIIIIIFFFFFFFF)V";

    private XaeroInjectionTargets() {
    }
}
