package com.ragemod.client.mixin;

import com.ragemod.client.Dismember;
import net.minecraft.client.model.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Model parcalarinin isimlerini ("left_arm" gibi) hatirla. */
@Mixin(ModelPart.class)
public abstract class ModelPartMixin {
    @Inject(method = "getChild", at = @At("RETURN"))
    private void ragemod$rememberName(String name, CallbackInfoReturnable<ModelPart> cir) {
        Dismember.rememberName(cir.getReturnValue(), name);
    }
}
