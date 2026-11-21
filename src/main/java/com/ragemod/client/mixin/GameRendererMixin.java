package com.ragemod.client.mixin;

import com.ragemod.client.CameraShake;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vurunca ekran sarsilsin. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"))
    private void ragemod$shake(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        CameraShake.apply(matrices, tickDelta);
    }
}
