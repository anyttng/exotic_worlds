package com.exoticworlds.compat.eminus.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.eminus.cell.cache.CellHandle;
import com.eminus.mesh.MeshListener;
import com.eminus.mesh.MeshService;
import com.exoticworlds.compat.eminus.CopyBuilds;
import com.exoticworlds.compat.eminus.EminusInjectionTargets;
import com.exoticworlds.compat.eminus.MeshServiceCopies;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(MeshService.class)
public class MeshServiceFailuresMixin {
    @WrapOperation(method = "build", at = @At(value = "INVOKE", target = EminusInjectionTargets.MESH_LISTENER_FAILED))
    private void toroidal$failEveryCopy(MeshListener listener, long key, long request, Operation<Void> original) {
        original.call(listener, key, request);
        CopyBuilds<CellHandle> copies = ((MeshServiceCopies) (Object) this).toroidal$copies();
        for (CopyBuilds.Member<CellHandle> copy : copies.delivered(request)) {
            original.call(listener, copy.key(), copy.request());
        }
    }
}
