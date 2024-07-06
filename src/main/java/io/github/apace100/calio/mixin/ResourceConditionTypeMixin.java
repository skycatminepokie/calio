package io.github.apace100.calio.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.apace100.calio.util.CalioResourceConditions;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ResourceConditionType.class)
public interface ResourceConditionTypeMixin {

    @ModifyExpressionValue(method = "lambda$static$2", at = @At(value = "INVOKE", target = "Lnet/fabricmc/fabric/api/resource/conditions/v1/ResourceConditions;getConditionType(Lnet/minecraft/util/Identifier;)Lnet/fabricmc/fabric/api/resource/conditions/v1/ResourceConditionType;"))
    private static ResourceConditionType<?> calio$applyAliases(ResourceConditionType<?> original, Identifier id) {
        return original != null
            ? original
            : ResourceConditions.getConditionType(CalioResourceConditions.ALIASES.resolveAlias(id, k -> ResourceConditions.getConditionType(k) != null));
    }

}
