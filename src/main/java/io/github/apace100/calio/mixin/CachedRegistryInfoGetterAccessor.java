package io.github.apace100.calio.mixin;

import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RegistryOps.CachedRegistryInfoGetter.class)
public interface CachedRegistryInfoGetterAccessor {

    @Final
    @Accessor
    RegistryWrapper.WrapperLookup getRegistriesLookup();

}
