package com.exoticworlds.client.shape.layout;

import java.util.List;

import com.exoticworlds.api.v1.option.WorldOption;
import com.exoticworlds.shape.layout.ShapeKind;
import com.exoticworlds.shape.torus.GuaranteedLand;
import com.exoticworlds.shape.torus.GuaranteedNetherComplexes;

import net.minecraft.network.chat.Component;

record DimensionOffer(Component title, List<ShapeKind> kinds, boolean spawnPoint, boolean netherScale,
        String torusOptionKey, WorldOption<Boolean> torusOption) {
    static final DimensionOffer OVERWORLD = new DimensionOffer(SettingsLines.OVERWORLD,
            List.of(ShapeKind.NORMAL, ShapeKind.TORUS, ShapeKind.CYLINDER), true, false,
            SettingsLines.GUARANTEED_LAND_KEY, GuaranteedLand.OPTION);

    static final DimensionOffer NETHER = new DimensionOffer(SettingsLines.NETHER,
            List.of(ShapeKind.FROM_OVERWORLD, ShapeKind.NORMAL, ShapeKind.TORUS, ShapeKind.CYLINDER), false, true,
            SettingsLines.GUARANTEED_NETHER_COMPLEXES_KEY, GuaranteedNetherComplexes.OPTION);
}
