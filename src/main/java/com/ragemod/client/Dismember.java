package com.ragemod.client;

import net.minecraft.block.Blocks;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Yaratik parcalanmasi: cani %75 / %50 / %25'in altina dustukce
 * sirayla bir uzvu (kol, bacak, wither kafasi...) kopar.
 */
public final class Dismember {
    private Dismember() {}

    /** Hangi parcalar hangi sirayla kopsun. Modelde olmayanlar atlanir. */
    private static final String[] ORDER = {
            "left_arm", "right_leg", "right_arm",           // insansi: zombi, iskelet, warden...
            "left_front_leg", "right_hind_leg", "right_front_leg", // dort ayaklilar
            "left_head", "right_head",                       // wither
            "left_wing", "right_wing"
    };
    private static final int MAX_LOST = 3;

    private static final Map<ModelPart, String> NAMES = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<EntityModel<?>, Map<String, ModelPart>> CACHE = new WeakHashMap<>();
    private static final Map<Integer, Integer> LAST_LOST = new HashMap<>();
    private static final List<ModelPart> HIDDEN = new ArrayList<>();

    private static final DustParticleEffect BLOOD = new DustParticleEffect(new Vector3f(0.55f, 0f, 0f), 1.8f);

    public static void rememberName(ModelPart part, String name) {
        if (part != null) NAMES.put(part, name);
    }

    public static void before(LivingEntity entity, EntityModel<?> model) {
        if (model == null || entity instanceof PlayerEntity || entity instanceof ArmorStandEntity) return;
        float max = entity.getMaxHealth();
        if (max <= 0) return;
        float frac = entity.getHealth() / max;
        int lost = frac < 0.25f ? 3 : frac < 0.5f ? 2 : frac < 0.75f ? 1 : 0;
        lost = Math.min(lost, MAX_LOST);

        int id = entity.getId();
        int prev = LAST_LOST.getOrDefault(id, 0);
        if (lost > prev) gore(entity);
        if (lost != prev) {
            if (LAST_LOST.size() > 4000) LAST_LOST.clear();
            LAST_LOST.put(id, lost);
        }
        if (lost == 0) return;

        Map<String, ModelPart> parts = partsOf(model);
        int n = 0;
        for (String name : ORDER) {
            if (n >= lost) break;
            ModelPart part = parts.get(name);
            if (part == null) continue;
            if (part.visible) {
                part.visible = false;
                HIDDEN.add(part);
            }
            n++;
        }
    }

    public static void after() {
        for (ModelPart p : HIDDEN) p.visible = true;
        HIDDEN.clear();
    }

    private static Map<String, ModelPart> partsOf(EntityModel<?> model) {
        Map<String, ModelPart> cached = CACHE.get(model);
        if (cached != null) return cached;
        Map<String, ModelPart> map = new HashMap<>();
        Class<?> c = model.getClass();
        while (c != null && c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                if (f.getType() != ModelPart.class) continue;
                try {
                    f.setAccessible(true);
                    ModelPart root = (ModelPart) f.get(model);
                    if (root == null) continue;
                    root.traverse().forEach(part -> {
                        String nm = NAMES.get(part);
                        if (nm != null) map.putIfAbsent(nm, part);
                    });
                } catch (Throwable ignored) {
                }
            }
            c = c.getSuperclass();
        }
        CACHE.put(model, map);
        return map;
    }

    private static void gore(LivingEntity e) {
        World w = e.getWorld();
        Random r = e.getRandom();
        double x = e.getX(), y = e.getY() + e.getHeight() * 0.6, z = e.getZ();
        BlockStateParticleEffect chunk = new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.getDefaultState());
        for (int i = 0; i < 30; i++) {
            w.addParticle(BLOOD, x, y, z, (r.nextDouble() - 0.5) * 0.6, r.nextDouble() * 0.4, (r.nextDouble() - 0.5) * 0.6);
        }
        for (int i = 0; i < 15; i++) {
            w.addParticle(chunk, x, y, z, (r.nextDouble() - 0.5) * 0.5, r.nextDouble() * 0.5, (r.nextDouble() - 0.5) * 0.5);
        }
        w.playSound(x, y, z, SoundEvents.ENTITY_SLIME_SQUISH, SoundCategory.HOSTILE, 1.0f, 0.6f, false);
    }
}
