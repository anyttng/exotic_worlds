package com.exoticworlds.compat.create.mixin;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.zurrtum.create.catnip.data.IntAttached;
import com.zurrtum.create.client.content.logistics.tableCloth.TableClothOverlayRenderer;
import com.zurrtum.create.infrastructure.component.ShoppingList;
import com.exoticworlds.compat.create.client.CreateClientFrame;
import com.exoticworlds.engine.fold.FoldedCopies;

import net.minecraft.core.BlockPos;

@Mixin(TableClothOverlayRenderer.class)
public abstract class TableClothOverlayRendererMixin {
    @ModifyExpressionValue(method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lcom/zurrtum/create/content/logistics/tableCloth/ShoppingListItem;"
                            + "getList(Lnet/minecraft/world/item/ItemStack;)"
                            + "Lcom/zurrtum/create/infrastructure/component/ShoppingList;"),
            require = 2,
            allow = 2)
    private static @Nullable ShoppingList exotic_worlds$purchaseKeysInTheViewerFrame(@Nullable ShoppingList canonical) {
        if (canonical == null) {
            return null;
        }

        List<IntAttached<BlockPos>> purchases = canonical.purchases();
        List<IntAttached<BlockPos>> folded = FoldedCopies.of(purchases, purchase -> {
            BlockPos stored = purchase.getValue();
            BlockPos nearest = CreateClientFrame.inViewerFrame(stored);
            return nearest == stored ? purchase : IntAttached.with(purchase.getFirst(), nearest);
        });

        return folded == purchases ? canonical
                : new ShoppingList(folded, canonical.shopOwner(), canonical.shopNetwork());
    }
}
