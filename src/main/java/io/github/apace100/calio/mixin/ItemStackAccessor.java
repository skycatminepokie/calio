package io.github.apace100.calio.mixin;

import com.mojang.serialization.DataResult;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ItemStack.class)
public interface ItemStackAccessor {

    @Invoker
    static DataResult<ItemStack> callValidate(ItemStack stack) {
        throw new AssertionError();
    }

    @Accessor("count")
    int getCountOverride();

}
