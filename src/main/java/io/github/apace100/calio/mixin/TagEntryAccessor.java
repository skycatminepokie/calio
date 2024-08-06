package io.github.apace100.calio.mixin;

import net.minecraft.registry.tag.TagEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(TagEntry.class)
public interface TagEntryAccessor {

    @Invoker
    Codecs.TagEntryId callGetIdForCodec();

    @Accessor
    Identifier getId();

    @Accessor
    boolean isTag();

    @Accessor
    boolean isRequired();

}
