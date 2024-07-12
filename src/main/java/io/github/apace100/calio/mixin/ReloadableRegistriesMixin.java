package io.github.apace100.calio.mixin;

import io.github.apace100.calio.Calio;
import net.minecraft.registry.*;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Unit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(value = ReloadableRegistries.class, priority = 900)
public abstract class ReloadableRegistriesMixin {

    @Inject(method = "reload", at = @At(value = "INVOKE", target = "Lnet/minecraft/registry/ReloadableRegistries$ReloadableWrapperLookup;getOps(Lcom/mojang/serialization/DynamicOps;)Lnet/minecraft/registry/RegistryOps;", shift = At.Shift.AFTER))
    private static void calio$cacheNamespaces(CombinedDynamicRegistries<ServerDynamicRegistryType> dynamicRegistries, ResourceManager resourceManager, Executor prepareExecutor, CallbackInfoReturnable<CompletableFuture<CombinedDynamicRegistries<ServerDynamicRegistryType>>> cir) {
        Calio.LOADED_NAMESPACES.put(Unit.INSTANCE, resourceManager.getAllNamespaces());
    }

}
