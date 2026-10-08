package com.exoticworlds.migration;

import com.exoticworlds.ExoticWorlds;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ModMismatchEvent;

@EventBusSubscriber(modid = ExoticWorlds.MODID)
public final class FormerModResolution {
    @SubscribeEvent
    public static void resolveFormerMod(ModMismatchEvent event) {
        if (event.getVersionDifference(FormerNamespace.NAMESPACE).isPresent()) {
            event.markResolved(FormerNamespace.NAMESPACE);
        }
    }

    private FormerModResolution() {
    }
}
