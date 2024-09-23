package io.github.apace100.calio.mixin;

import io.github.apace100.calio.CalioServer;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.registry.tag.TagManagerLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.List;

@Mixin(TagManagerLoader.class)
public abstract class TagManagerLoaderMixin {

    @Shadow private List<TagManagerLoader.RegistryTags<?>> registryTags;

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method = "method_40098", at = @At("RETURN"))
    private void calio$cacheRegistryTags(List<?> list, Void void_, CallbackInfo ci) {

        CalioServer.REGISTRY_TAGS.clear();

        this.registryTags.forEach(entry -> entry.tags().forEach((id, entries) ->
            CalioServer.REGISTRY_TAGS.put(TagKey.of(entry.key(), id), (Collection) entries))
        );

    }

}
