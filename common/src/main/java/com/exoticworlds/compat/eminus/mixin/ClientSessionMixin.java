package com.exoticworlds.compat.eminus.mixin;

import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.eminus.client.session.ClientSession;
import com.eminus.session.DimensionRuntime;
import com.exoticworlds.compat.eminus.CellFold;
import com.exoticworlds.compat.eminus.CellFoldHolder;
import com.exoticworlds.compat.eminus.EminusShapes;
import com.mojang.logging.LogUtils;

@Mixin(ClientSession.class)
public class ClientSessionMixin {
    @Unique
    private static final Logger LOGGER = LogUtils.getLogger();

    @Shadow
    private static DimensionRuntime runtime;

    @Shadow
    private static void restartSession() {
    }

    @Inject(method = {"tick", "submitChunk"}, at = @At("HEAD"))
    private static void toroidal$reopenUnderPublishedShape(CallbackInfo ci) {
        DimensionRuntime current = runtime;
        if (current == null) {
            return;
        }

        CellFold opened = CellFoldHolder.foldOf(current);
        CellFold published = EminusShapes.publishedFold(current.identity().dimension());
        if (published.repeats() && !opened.equals(published)) {
            LOGGER.info("[eminus-compat] session_restart dimension={} opened={} published={}",
                    current.identity().dimension(), opened, published);
            restartSession();
        }
    }
}
