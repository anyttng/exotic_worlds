package com.exoticworlds.compat.eminus.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.eminus.cell.cache.CellHandle;
import com.eminus.mesh.MeshService;
import com.exoticworlds.compat.eminus.MeshServiceCopies;

@Mixin(MeshService.class)
public class MeshServiceRequestMixin {
    @Inject(method = "request(JLcom/eminus/cell/cache/CellHandle;IJF)V", at = @At("HEAD"), cancellable = true)
    private void exotic_worlds$joinAWaitingCopy(long key, @Nullable CellHandle held, int references, long request,
            float priority, CallbackInfo ci) {
        if (((MeshServiceCopies) (Object) this).exotic_worlds$joined(key, held, references, request, priority,
                false)) {
            ci.cancel();
        }
    }
}
