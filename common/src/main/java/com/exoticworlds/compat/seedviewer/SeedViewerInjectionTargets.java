package com.exoticworlds.compat.seedviewer;

public final class SeedViewerInjectionTargets {
    public static final String CONSTRUCTOR = "<init>";

    public static final String FROM_LEVEL = "(Lnet/minecraft/server/level/ServerLevel;)V";

    public static final String FROM_BIOME_SOURCE = "(Lnet/minecraft/world/level/biome/BiomeSource;"
            + "Lnet/minecraft/world/level/biome/Climate$Sampler;)V";

    public static final String FROM_RANDOM_STATE = "(Lnet/minecraft/world/level/levelgen/RandomState;)V";

    public static final String CONTEXT_DESCRIPTOR = "(JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;"
            + "Lnet/acenia/seedviewer/client/worldgen/BiomeSampler;"
            + "Lnet/acenia/seedviewer/client/render/BiomeColorProvider;"
            + "Lnet/acenia/seedviewer/client/model/BiomeCatalog;Ljava/util/Map;Ljava/lang/String;)V";

    public static final String DRAW_TILES = "drawTiles";

    public static final String DISABLE_SCISSOR = "Lnet/minecraft/client/gui/GuiGraphics;disableScissor()V";

    private SeedViewerInjectionTargets() {
    }
}
