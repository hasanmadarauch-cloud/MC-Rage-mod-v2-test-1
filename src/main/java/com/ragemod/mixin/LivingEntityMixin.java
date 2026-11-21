package com.ragemod.mixin;

import com.ragemod.RageManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Shadow protected boolean dead;

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void ragemod$onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.getWorld().isClient || RageManager.REENTRY) return;

        if (RageManager.onDamage(self, source, amount)) {
            cir.setReturnValue(false);
            return;
        }

        float factor = RageManager.incomingFactor(self);
        if (factor < 1f) {
            RageManager.REENTRY = true;
            try {
                cir.setReturnValue(self.damage(source, amount * factor));
            } finally {
                RageManager.REENTRY = false;
            }
        }
    }

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void ragemod$onDeath(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.getWorld().isClient || this.dead || self.isRemoved()) return;
        RageManager.onDeath(self, source);
    }
}
