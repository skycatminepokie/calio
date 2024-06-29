package io.github.apace100.calio.mixin;

import io.github.apace100.calio.util.CalioResourceConditions;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.resource.SinglePreparationResourceReloader;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SinglePreparationResourceReloader.class)
public abstract class SinglePreparationResourceReloaderMixin implements ResourceReloader {

    @Inject(method = "method_18791", at = @At(value = "INVOKE", target = "Lnet/minecraft/resource/SinglePreparationResourceReloader;prepare(Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/util/profiler/Profiler;)Ljava/lang/Object;"))
    private void calio$cacheNamespaces(ResourceManager manager, Profiler profiler, CallbackInfoReturnable<Object> cir) {
        CalioResourceConditions.NAMESPACES.set(manager.getAllNamespaces());
    }

    @Inject(method = "method_18790", at = @At(value = "INVOKE", target = "Lnet/minecraft/resource/SinglePreparationResourceReloader;apply(Ljava/lang/Object;Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/util/profiler/Profiler;)V"))
    private void calio$resetNamespaceCache(ResourceManager manager, Profiler profiler, Object prepared, CallbackInfo ci) {
        CalioResourceConditions.NAMESPACES.remove();
    }

}
