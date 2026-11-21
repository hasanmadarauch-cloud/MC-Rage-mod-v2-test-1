package com.ragemod.mixin;

import com.ragemod.RageManager;
import com.ragemod.RageShared;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Etap 3'teki oyuncu ruh kumunda yavaslamaz. */
@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "getVelocityMultiplier", at = @At("HEAD"), cancellable = true)
    private void ragemod$noSoulSandSlow(CallbackInfoReturnable<Float> cir) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof PlayerEntity p)) return;
        int stage = self.getWorld().isClient
                ? (p.isMainPlayer() ? RageShared.clientStage : 0)
                : RageManager.peekStage(p);
        if (stage >= 3) cir.setReturnValue(1.0f);
    }
}
