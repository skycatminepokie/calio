package io.github.apace100.calio.mixin;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(RegistryEntryList.Named.class)
public interface RegistryEntryListNamedAccessor<T> {

    @Invoker
    List<RegistryEntry<T>> callGetEntries();

}
