package com.ragemod;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

/** Firlatilan blok (patlar) veya alet (hasar verir, sonra yere duser). */
public class RageThrownItemEntity extends ThrownItemEntity {
    private boolean explosive = false;
    private float power = 2f;

    public RageThrownItemEntity(EntityType<? extends RageThrownItemEntity> type, World world) {
        super(type, world);
    }

    public RageThrownItemEntity(World world, LivingEntity owner, ItemStack stack, boolean explosive, float power) {
        super(RageMod.THROWN_ITEM, owner, world);
        this.setItem(stack);
        this.explosive = explosive;
        this.power = power;
    }

    @Override
    protected Item getDefaultItem() {
        return Items.STONE;
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);
        if (!(this.getWorld() instanceof ServerWorld w)) return;
        Entity owner = this.getOwner();

        if (explosive) {
            w.createExplosion(owner != null ? owner : this, getX(), getY(), getZ(), power, RageMod.EXPLOSION_TYPE);
            RageManager.blood(w, getX(), getY(), getZ(), 25);
        } else {
            if (hitResult instanceof EntityHitResult eh && eh.getEntity() != owner) {
                Entity target = eh.getEntity();
                target.damage(this.getDamageSources().thrown(this, owner), power);
                w.playSound(null, target.getBlockPos(), SoundEvents.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1f, 0.8f);
            } else {
                w.playSound(null, getBlockPos(), SoundEvents.ITEM_TRIDENT_HIT_GROUND, SoundCategory.PLAYERS, 1f, 1f);
            }
            ItemStack stack = this.getStack();
            if (!stack.isEmpty()) {
                ItemEntity drop = new ItemEntity(w, getX(), getY() + 0.2, getZ(), stack.copy());
                drop.setToDefaultPickupDelay();
                w.spawnEntity(drop);
            }
        }
        this.discard();
    }
}
