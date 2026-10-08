package com.exoticworlds.migration.mixin;

import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.migration.FormerNamespace;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

@Mixin(MappedRegistry.class)
public class MappedRegistryMixin<T> {
    @ModifyReturnValue(method = "get(Lnet/minecraft/resources/Identifier;)Ljava/util/Optional;", at = @At("RETURN"))
    private Optional<Holder.Reference<T>> toroidal$getTwinById(Optional<Holder.Reference<T>> found,
            @Local(argsOnly = true) Identifier id) {
        if (found.isPresent()) {
            return found;
        }

        Identifier twin = FormerNamespace.twinOf(id);
        return twin == null ? found : this.toroidal$registry().get(twin);
    }

    @ModifyReturnValue(method = "get(Lnet/minecraft/resources/ResourceKey;)Ljava/util/Optional;", at = @At("RETURN"))
    private Optional<Holder.Reference<T>> toroidal$getTwinByKey(Optional<Holder.Reference<T>> found,
            @Local(argsOnly = true) ResourceKey<T> key) {
        if (found.isPresent()) {
            return found;
        }

        ResourceKey<T> twin = FormerNamespace.twinOf(key);
        return twin == null ? found : this.toroidal$registry().get(twin);
    }

    @ModifyReturnValue(method = "getValue(Lnet/minecraft/resources/Identifier;)Ljava/lang/Object;", at = @At("RETURN"))
    private @Nullable T toroidal$getTwinValueById(@Nullable T found, @Local(argsOnly = true) Identifier id) {
        if (found != null) {
            return found;
        }

        Identifier twin = FormerNamespace.twinOf(id);
        return twin == null ? found : this.toroidal$registry().getValue(twin);
    }

    @ModifyReturnValue(method = "getValue(Lnet/minecraft/resources/ResourceKey;)Ljava/lang/Object;", at = @At("RETURN"))
    private @Nullable T toroidal$getTwinValueByKey(@Nullable T found, @Local(argsOnly = true) ResourceKey<T> key) {
        if (found != null) {
            return found;
        }

        ResourceKey<T> twin = FormerNamespace.twinOf(key);
        return twin == null ? found : this.toroidal$registry().getValue(twin);
    }

    @ModifyReturnValue(method = "containsKey(Lnet/minecraft/resources/Identifier;)Z", at = @At("RETURN"))
    private boolean toroidal$containsTwinById(boolean found, @Local(argsOnly = true) Identifier id) {
        if (found) {
            return true;
        }

        Identifier twin = FormerNamespace.twinOf(id);
        return twin != null && this.toroidal$registry().containsKey(twin);
    }

    @ModifyReturnValue(method = "containsKey(Lnet/minecraft/resources/ResourceKey;)Z", at = @At("RETURN"))
    private boolean toroidal$containsTwinByKey(boolean found, @Local(argsOnly = true) ResourceKey<T> key) {
        if (found) {
            return true;
        }

        ResourceKey<T> twin = FormerNamespace.twinOf(key);
        return twin != null && this.toroidal$registry().containsKey(twin);
    }

    @Unique
    @SuppressWarnings("unchecked")
    private MappedRegistry<T> toroidal$registry() {
        return (MappedRegistry<T>) (Object) this;
    }
}
