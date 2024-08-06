package io.github.apace100.calio.mixin;

import net.minecraft.registry.RegistryOps;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RegistryOps.class)
public interface RegistryOpsAccessor {

    @Final
    @Accessor
    RegistryOps.RegistryInfoGetter getRegistryInfoGetter();

}
