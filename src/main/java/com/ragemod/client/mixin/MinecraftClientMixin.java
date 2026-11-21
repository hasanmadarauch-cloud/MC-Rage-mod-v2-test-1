package com.ragemod.client.mixin;

import com.ragemod.client.RageModClient;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Rage modunda sag tik (M2) blok koymak yerine tutma/firlatma yapsin. */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void ragemod$blockUse(CallbackInfo ci) {
        if (RageModClient.shouldBlockUse((MinecraftClient) (Object) this)) {
            ci.cancel();
        }
    }
}
