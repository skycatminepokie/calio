package io.github.apace100.calio.mixin;

import net.minecraft.util.collection.WeightedList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(WeightedList.class)
public interface WeightedListAccessor<E> {

    @Final
    @Accessor
    List<WeightedList.Entry<E>> getEntries();

}
