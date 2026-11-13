package com.exoticworlds.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.zurrtum.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.zurrtum.create.content.logistics.factoryBoard.ServerFactoryPanelBehaviour;
import com.exoticworlds.accessors.SeamKeyedBlockEntity;
import com.exoticworlds.compat.create.CreateFactoryPanelFold;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;

@Mixin(FactoryPanelBlockEntity.class)
public abstract class FactoryPanelBlockEntityMixin implements SeamKeyedBlockEntity {
    @Inject(method = "read", at = @At("RETURN"))
    private void exotic_worlds$rekeyOnRead(ValueInput view, boolean clientPacket, CallbackInfo ci) {
        exotic_worlds$rekey();
    }

    @Override
    public void exotic_worlds$rekey() {
        FactoryPanelBlockEntity panelBE = (FactoryPanelBlockEntity) (Object) this;
        if (!(panelBE.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        for (ServerFactoryPanelBehaviour panel : panelBE.panels.values()) {
            CreateFactoryPanelFold.canonicalise(serverLevel, panel);
        }
    }
}
