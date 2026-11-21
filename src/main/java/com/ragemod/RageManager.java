package com.ragemod;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.AxeItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolItem;
import net.minecraft.item.TridentItem;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.util.*;

public final class RageManager {
    private RageManager() {}

    // =====================================================================
    //                    DENGE AYARLARI (index = etap 1,2,3)
    // =====================================================================
    static final int FREEZE_NORMAL = 20 * 120;   // 2 dakika metre azalmaz
    static final int FREEZE_PET = 20 * 360;      // evcil hayvan olduyse 6 dakika
    static final float METER_DRAIN_SECONDS = 60f;// vurmazsan metre 60 sn'de biter
    static final int SCREAM_COOLDOWN = 20 * 20;  // 20 sn

    //                                    -    E1     E2     E3
    static final double[] HEALTH_EXTRA = {0, 0.5,   2.0,   4.0};   // can x1.5 / x3 / x5
    static final double[] ARMOR        = {0, 10,    20,    30};
    static final double[] TOUGHNESS    = {0, 4,     12,    20};
    static final double[] KB_RESIST    = {0, 0.3,   0.6,   1.0};
    static final double[] ATTACK_BONUS = {0, 2,     5,     10};    // yumruk hasari bonusu
    static final float[]  BLOCK_CHANCE = {0, 0.4f,  0.6f,  0.85f}; // yaratik hasarini engelleme
    static final float[]  DMG_TAKEN    = {1, 1f,    1f,    0.4f};  // kalan hasar carpani
    static final float[]  SCREAM_DMG   = {0, 4,     8,     12};
    static final float[]  KICK_DMG     = {0, 5,     9,     14};
    static final double[] KICK_KB      = {0, 2.0,   3.0,   4.0};
    static final double[] SPRINT_BONUS = {0, 0.4,   0.6,   0.9};   // max kosu hizi bonusu
    static final float[]  EXPLO_MULT   = {0, 0.8f,  1.2f,  1.6f};  // patlama gucu carpani
    static final float[]  BRAVE        = {0, 0.06f, 0.03f, 0f};    // yaratigin cesaretlenme ihtimali
    static final float[]  HIT_FILL     = {0, 1.2f,  0.7f,  0f};    // vurunca sonraki etap bari
    static final float[]  DMG_FILL     = {0, 2.0f,  1.2f,  0f};    // hasar alinca sonraki etap bari
    static final float[]  KILL_FILL    = {0, 5f,    3f,    0f};
    static final float[]  PUNCH_KB     = {0, 0.4f,  0.8f,  1.3f};  // yumruk ekstra savurma
    static final float[]  WAVE_DMG     = {0, 0,     8,     12};
    static final int[]    SMASH_RADIUS = {0, 0,     2,     3};
    // =====================================================================

    private static final UUID HEALTH_ID = UUID.fromString("6b1e3c2a-1d7e-4c51-9a4f-1a2b3c4d5e01");
    private static final UUID ARMOR_ID = UUID.fromString("6b1e3c2a-1d7e-4c51-9a4f-1a2b3c4d5e02");
    private static final UUID TOUGH_ID = UUID.fromString("6b1e3c2a-1d7e-4c51-9a4f-1a2b3c4d5e03");
    private static final UUID KB_ID = UUID.fromString("6b1e3c2a-1d7e-4c51-9a4f-1a2b3c4d5e04");
    private static final UUID SPEED_ID = UUID.fromString("6b1e3c2a-1d7e-4c51-9a4f-1a2b3c4d5e05");
    private static final UUID ATTACK_ID = UUID.fromString("6b1e3c2a-1d7e-4c51-9a4f-1a2b3c4d5e06");

    public static final DustParticleEffect BLOOD = new DustParticleEffect(new Vector3f(0.55f, 0f, 0f), 1.6f);
    private static final DustParticleEffect AURA = new DustParticleEffect(new Vector3f(1f, 0.1f, 0.05f), 1.2f);
    private static final DustParticleEffect AURA3 = new DustParticleEffect(new Vector3f(0.15f, 0.9f, 0.95f), 1.4f);

    private static final Map<UUID, RageData> DATA = new HashMap<>();
    private static final List<ThrownMob> THROWN = new ArrayList<>();
    private static final List<Wave> WAVES = new ArrayList<>();
    private static final Map<Entity, Integer> BRAVE_UNTIL = new WeakHashMap<>();

    /** Yetenek hasari verilirken true: yumruk efektleri tekrar tetiklenmesin. */
    private static boolean abilityHit = false;
    /** Hasar azaltma icin damage() tekrar cagrilirken true. */
    public static boolean REENTRY = false;

    private static final class ThrownMob {
        final Entity entity;
        final ServerPlayerEntity thrower;
        final boolean big;
        final int stage;
        int age = 0;
        ThrownMob(Entity e, ServerPlayerEntity t, boolean big, int stage) { entity = e; thrower = t; this.big = big; this.stage = stage; }
    }

    private static final class Wave {
        final ServerWorld world;
        final ServerPlayerEntity owner;
        final Vec3d dir;
        final int stage;
        final Set<Integer> hit = new HashSet<>();
        Vec3d pos;
        int life = 16;
        Wave(ServerWorld w, ServerPlayerEntity o, Vec3d pos, Vec3d dir, int stage) { world = w; owner = o; this.pos = pos; this.dir = dir; this.stage = stage; }
    }

    public static RageData get(PlayerEntity p) {
        return DATA.computeIfAbsent(p.getUuid(), u -> new RageData());
    }

    public static int peekStage(PlayerEntity p) {
        RageData d = DATA.get(p.getUuid());
        return d == null ? 0 : d.stage;
    }

    public static boolean isRaging(Entity e) {
        return e instanceof ServerPlayerEntity p && get(p).active();
    }

    // ================================================================== tick

    public static void tick(MinecraftServer server) {
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            tickPlayer(p);
        }
        tickThrownMobs();
        tickWaves();
    }

    private static void tickPlayer(ServerPlayerEntity p) {
        RageData d = get(p);

        if (!p.isAlive()) {
            if (d.active()) endRage(p, d, false);
            d.bar = 0;
            d.petRage = false;
            if (p.age % 10 == 0) sync(p, d);
            return;
        }

        if (d.screamCooldown > 0) d.screamCooldown--;
        if (d.kickCooldown > 0) d.kickCooldown--;
        if (d.throwCooldown > 0) d.throwCooldown--;
        if (d.slamCooldown > 0) d.slamCooldown--;
        if (d.smashCooldown > 0) d.smashCooldown--;
        if (d.waveCooldown > 0) d.waveCooldown--;
        d.ticksSinceHit++;
        d.ticksSinceDamaged++;

        if (!d.active()) {
            if (d.bar > 0 && d.bar < 100 && !d.petRage && d.ticksSinceDamaged > 400) {
                d.bar = Math.max(0, d.bar - 0.05f);
            }
        } else {
            tickActive(p, d);
        }

        if (p.age % 2 == 0) sync(p, d);
    }

    private static void tickActive(ServerPlayerEntity p, RageData d) {
        ServerWorld w = (ServerWorld) p.getWorld();
        int s = d.stage;

        // Metre
        if (d.freezeTicks > 0) {
            d.freezeTicks--;
        } else if (d.ticksSinceHit > 60) {
            d.meter -= 100f / (METER_DRAIN_SECONDS * 20f);
        }
        if (d.meter <= 0) {
            endRage(p, d, true);
            return;
        }

        // Sonraki etap bari dolu: wither "sarj" sesi
        if (s < 3 && d.bar >= 100f && p.age % 40 == 0) {
            w.playSound(null, p.getBlockPos(), SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 1.0f, 0.5f);
            w.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_WITHER_AMBIENT, SoundCategory.PLAYERS, 0.7f, 0.5f);
        }

        // Kostukca hizlanma (etap 3: yurume yavas, kosu hizli)
        float rampRate = s >= 3 ? 0.02f : 0.01f;
        if (p.isSprinting()) d.sprintRamp = Math.min(1f, d.sprintRamp + rampRate);
        else d.sprintRamp = Math.max(0f, d.sprintRamp - 0.05f);
        applySpeed(p, d);
        if (d.sprintRamp >= 1f && p.isSprinting()) smashWalls(p, w, s);

        tickGrab(p, d);

        // Etap 3 dash hasari
        if (d.dashTicks > 0) {
            d.dashTicks--;
            for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, p.getBoundingBox().expand(1.3),
                    e -> e != p && e.isAlive() && !isOwnedBy(e, p) && !d.dashHit.contains(e.getId()))) {
                d.dashHit.add(e.getId());
                abilityDamage(e, p, 10f);
                Vec3d push = e.getPos().subtract(p.getPos()).normalize();
                e.takeKnockback(1.5, -push.x, -push.z);
                e.velocityModified = true;
            }
        }

        if (p.age % 5 == 0) scareMobs(p, d, w);

        if (s >= 3) {
            tickSoulSand(p, d, w);
            if (p.getRandom().nextInt(300) == 0) {
                w.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_WITHER_AMBIENT, SoundCategory.PLAYERS, 1.5f, 0.6f);
            }
            if (p.age % 3 == 0) {
                w.spawnParticles(AURA3, p.getX(), p.getY() + 1.0, p.getZ(), 3, 0.4, 0.7, 0.4, 0.0);
                w.spawnParticles(ParticleTypes.SOUL, p.getX(), p.getY() + 0.2, p.getZ(), 1, 0.4, 0.1, 0.4, 0.02);
            }
        } else if (p.age % 4 == 0) {
            w.spawnParticles(AURA, p.getX(), p.getY() + 1.0, p.getZ(), s == 2 ? 6 : 3, 0.4, 0.6, 0.4, 0.0);
        }

        d.lastX = p.getX();
        d.lastZ = p.getZ();
    }

    // ================================================================== actions

    public static void handleAction(ServerPlayerEntity p, int action, int arg) {
        if (!p.isAlive()) return;
        RageData d = get(p);
        switch (action) {
            case RageNet.A_SCREAM -> {
                if (!d.active()) {
                    if (d.bar >= 100f) activate(p, d);
                    else p.sendMessage(Text.literal("Henüz yeterince sinirli değilsin... (" + (int) d.bar + "%)").formatted(Formatting.GRAY), true);
                } else if (d.stage < 3 && d.bar >= 100f) {
                    stageUp(p, d);
                } else if (d.screamCooldown <= 0) {
                    scream(p, d);
                } else {
                    p.sendMessage(Text.literal("Bağırmak için " + (d.screamCooldown / 20 + 1) + " sn bekle").formatted(Formatting.RED), true);
                }
            }
            case RageNet.A_DEACTIVATE -> { if (d.active()) endRage(p, d, true); }
            case RageNet.A_KICK -> { if (d.active()) kick(p, d); }
            case RageNet.A_GRAB -> { if (d.active()) grab(p, d); }
            case RageNet.A_THROW_MOB -> { if (d.active()) throwMob(p, d); }
            case RageNet.A_THROW_TOOL -> { if (d.active()) throwTool(p, d); }
            case RageNet.A_THROW_BLOCK -> { if (d.active()) throwBlock(p, d, arg); }
            case RageNet.A_SUPERJUMP -> {
                if (d.active()) {
                    ServerWorld w = (ServerWorld) p.getWorld();
                    w.spawnParticles(ParticleTypes.CLOUD, p.getX(), p.getY(), p.getZ(), 15, 0.5, 0.1, 0.5, 0.05);
                    w.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.4f, 1.6f);
                }
            }
            case RageNet.A_SLAM -> { if (d.active() && d.slamCooldown <= 0) slam(p, d); }
            case RageNet.A_DASH -> {
                if (d.active()) {
                    ServerWorld w = (ServerWorld) p.getWorld();
                    w.spawnParticles(d.stage >= 3 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.CLOUD,
                            p.getX(), p.getY() + 0.5, p.getZ(), 12, 0.3, 0.3, 0.3, 0.02);
                    w.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1f, 0.6f);
                    if (d.stage >= 3) {
                        d.dashTicks = 8;
                        d.dashHit.clear();
                    }
                }
            }
            case RageNet.A_PUNCH_WALL -> { if (d.stage >= 2 && d.smashCooldown <= 0) punchWall(p, d); }
            case RageNet.A_AIR_WAVE -> { if (d.stage >= 2 && d.waveCooldown <= 0) airWave(p, d); }
            default -> {}
        }
    }

    // ================================================================== activate / stages / end

    private static void activate(ServerPlayerEntity p, RageData d) {
        d.stage = 1;
        d.meter = 100f;
        d.freezeTicks = d.petRage ? FREEZE_PET : FREEZE_NORMAL;
        d.petRage = false;
        d.bar = 0f;
        d.ticksSinceHit = 0;
        d.sprintRamp = 0f;
        applyStageStats(p, d.stage);
        p.setHealth(p.getMaxHealth());

        title(p, "RAGE", Formatting.DARK_RED);
        scream(p, d);
    }

    private static void stageUp(ServerPlayerEntity p, RageData d) {
        d.stage++;
        d.bar = 0f;
        d.meter = 100f;
        d.freezeTicks = Math.max(d.freezeTicks, FREEZE_NORMAL);
        d.ticksSinceHit = 0;
        applyStageStats(p, d.stage);
        p.setHealth(p.getMaxHealth());

        ServerWorld w = (ServerWorld) p.getWorld();
        w.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS, 2.5f, d.stage == 3 ? 0.7f : 1.0f);
        w.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, p.getX(), p.getY() + 1, p.getZ(), 2, 0.5, 0.5, 0.5, 0);
        if (d.stage == 3) {
            w.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, p.getX(), p.getY() + 1, p.getZ(), 80, 1.5, 1.5, 1.5, 0.15);
            title(p, "RAGE III", Formatting.DARK_AQUA);
        } else {
            w.spawnParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 1, p.getZ(), 60, 1.5, 1.5, 1.5, 0.1);
            title(p, "RAGE II", Formatting.RED);
        }
        shake(p, 3f);

        // Bedava bir bagiris (bekleme suresi sifirlanir)
        d.screamCooldown = 0;
        scream(p, d);
    }

    private static void applyStageStats(ServerPlayerEntity p, int s) {
        addMod(p, EntityAttributes.GENERIC_MAX_HEALTH, HEALTH_ID, HEALTH_EXTRA[s], EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
        addMod(p, EntityAttributes.GENERIC_ARMOR, ARMOR_ID, ARMOR[s], EntityAttributeModifier.Operation.ADDITION);
        addMod(p, EntityAttributes.GENERIC_ARMOR_TOUGHNESS, TOUGH_ID, TOUGHNESS[s], EntityAttributeModifier.Operation.ADDITION);
        addMod(p, EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, KB_ID, KB_RESIST[s], EntityAttributeModifier.Operation.ADDITION);
        addMod(p, EntityAttributes.GENERIC_ATTACK_DAMAGE, ATTACK_ID, ATTACK_BONUS[s], EntityAttributeModifier.Operation.ADDITION);
    }

    private static void endRage(ServerPlayerEntity p, RageData d, boolean debuff) {
        int oldStage = d.stage;
        d.stage = 0;
        d.bar = 0f;
        d.meter = 0f;
        d.freezeTicks = 0;
        d.sprintRamp = 0f;
        d.grabbed = null;
        d.dashTicks = 0;
        removeAllMods(p);
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());

        if (debuff && p.isAlive()) {
            int extra = Math.max(0, oldStage - 1) * 100;
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 200 + extra / 2, 0));
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 300 + extra, 1));
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 200 + extra / 2, 0));
            p.sendMessage(Text.literal("Sinirin geçti... kendini bitkin hissediyorsun.").formatted(Formatting.DARK_GRAY), true);
            p.getWorld().playSound(null, p.getBlockPos(), SoundEvents.ENTITY_PLAYER_BREATH, SoundCategory.PLAYERS, 1f, 0.6f);
        }
        sync(p, d);
    }

    /** Giris/cikis/respawn: her seyi temizle. */
    public static void reset(ServerPlayerEntity p) {
        RageData d = get(p);
        d.stage = 0;
        d.meter = 0;
        d.freezeTicks = 0;
        d.grabbed = null;
        d.sprintRamp = 0;
        d.dashTicks = 0;
        removeAllMods(p);
    }

    // ================================================================== scream

    private static void scream(ServerPlayerEntity p, RageData d) {
        ServerWorld w = (ServerWorld) p.getWorld();
        int s = d.stage;
        double x = p.getX(), y = p.getY() + 1.0, z = p.getZ();

        w.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.PLAYERS, 2.0f, 0.7f);
        w.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.2f, 0.6f);
        w.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0, 0, 0, 0);
        w.spawnParticles(ParticleTypes.SONIC_BOOM, x, y, z, 1, 0, 0, 0, 0);
        for (int i = 0; i < 48; i++) {
            double a = i * (Math.PI * 2 / 48);
            w.spawnParticles(s >= 3 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.CLOUD,
                    x, y - 0.5, z, 0, Math.cos(a), 0.05, Math.sin(a), 0.9);
        }
        shake(p, 1.5f + s);

        Box box = p.getBoundingBox().expand(8);
        for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, box,
                e -> e != p && e.isAlive() && e != d.grabbed && !isOwnedBy(e, p) && e.squaredDistanceTo(p) <= 64)) {
            abilityDamage(e, p, SCREAM_DMG[s]);
            Vec3d push = e.getPos().subtract(p.getPos()).normalize();
            e.takeKnockback(1.0 + 0.4 * s, -push.x, -push.z);
            e.addVelocity(0, 0.3, 0);
            e.velocityModified = true;
        }

        // Sadece cam ve mesaleleri kir (8 blok)
        BlockPos c = p.getBlockPos();
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int dx = -8; dx <= 8; dx++) {
            for (int dy = -8; dy <= 8; dy++) {
                for (int dz = -8; dz <= 8; dz++) {
                    if (dx * dx + dy * dy + dz * dz > 64) continue;
                    m.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
                    BlockState st = w.getBlockState(m);
                    if (st.isAir()) continue;
                    String path = Registries.BLOCK.getId(st.getBlock()).getPath();
                    if (path.contains("glass") || path.contains("torch")) {
                        w.breakBlock(m, path.contains("torch"), p);
                    }
                }
            }
        }

        // Bufflar (etaba gore guclenir)
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 20 * 20, s - 1));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 20 * 20, s + 1));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 20 * 20, s >= 3 ? 1 : 0));

        if (d.grabbed instanceof LivingEntity le) {
            le.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 20 * 10, 0));
            le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20 * 10, 0));
            blood(w, le.getX(), le.getY() + le.getHeight() / 2, le.getZ(), 15);
        }

        d.meter = Math.min(100f, d.meter + 12f);
        d.ticksSinceHit = 0;
        d.screamCooldown = SCREAM_COOLDOWN;
    }

    // ================================================================== kick

    private static void kick(ServerPlayerEntity p, RageData d) {
        if (d.kickCooldown > 0) return;
        d.kickCooldown = 14;
        p.swingHand(Hand.MAIN_HAND, true);
        Entity t = raycastEntity(p, 4.5);
        ServerWorld w = (ServerWorld) p.getWorld();
        if (t instanceof LivingEntity le && t != d.grabbed) {
            Vec3d look = p.getRotationVec(1f);
            abilityDamage(le, p, KICK_DMG[d.stage]);
            le.takeKnockback(KICK_KB[d.stage], -look.x, -look.z);
            le.addVelocity(0, 0.35, 0);
            le.velocityModified = true;
            w.playSound(null, le.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundCategory.PLAYERS, 1.2f, 0.7f);
            w.playSound(null, le.getBlockPos(), SoundEvents.ENTITY_IRON_GOLEM_ATTACK, SoundCategory.PLAYERS, 1f, 0.8f);
            w.spawnParticles(ParticleTypes.EXPLOSION, le.getX(), le.getY() + le.getHeight() / 2, le.getZ(), 1, 0, 0, 0, 0);
            shake(p, 1.2f + 0.4f * d.stage);
        } else {
            w.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_WEAK, SoundCategory.PLAYERS, 1f, 0.8f);
        }
    }

    // ================================================================== stage 2+: wall punch & air wave

    private static void punchWall(ServerPlayerEntity p, RageData d) {
        HitResult hr = p.raycast(5.0, 1f, false);
        if (!(hr instanceof BlockHitResult bhr) || hr.getType() != HitResult.Type.BLOCK) {
            airWave(p, d); // bloga bakmiyorsa hava dalgasi
            return;
        }
        d.smashCooldown = d.stage >= 3 ? 8 : 14;
        ServerWorld w = (ServerWorld) p.getWorld();
        BlockPos center = bhr.getBlockPos();
        int r = SMASH_RADIUS[d.stage];
        float maxHardness = d.stage >= 3 ? 55f : 30f; // etap 3 obsidyeni de kirar
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dy * dy + dz * dz > r * r + 1) continue;
                    m.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState st = w.getBlockState(m);
                    if (st.isAir() || !st.getFluidState().isEmpty()) continue;
                    if (w.getBlockEntity(m) != null) continue;
                    float h = st.getHardness(w, m);
                    if (h < 0 || h > maxHardness) continue;
                    w.breakBlock(m, p.getRandom().nextFloat() < 0.25f, p);
                }
            }
        }
        Vec3d c = Vec3d.ofCenter(center);
        w.playSound(null, center, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.3f, 0.7f);
        w.playSound(null, center, SoundEvents.ENTITY_IRON_GOLEM_ATTACK, SoundCategory.PLAYERS, 1.5f, 0.5f);
        w.spawnParticles(d.stage >= 3 ? ParticleTypes.EXPLOSION_EMITTER : ParticleTypes.EXPLOSION, c.x, c.y, c.z, 3, 0.8, 0.8, 0.8, 0);
        for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, new Box(center).expand(r + 1),
                e -> e != p && e.isAlive() && !isOwnedBy(e, p))) {
            abilityDamage(e, p, d.stage >= 3 ? 10f : 6f);
        }
        p.swingHand(Hand.MAIN_HAND, true);
        shake(p, 2.5f + d.stage * 0.5f);
    }

    private static void airWave(ServerPlayerEntity p, RageData d) {
        if (d.waveCooldown > 0) return;
        d.waveCooldown = d.stage >= 3 ? 10 : 20;
        ServerWorld w = (ServerWorld) p.getWorld();
        Vec3d look = p.getRotationVec(1f);
        WAVES.add(new Wave(w, p, p.getEyePos().add(look), look, d.stage));
        p.swingHand(Hand.MAIN_HAND, true);
        w.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.5f, 0.5f);
        w.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS, 1.2f, 1.4f);
        shake(p, 1.2f);
    }

    private static void tickWaves() {
        Iterator<Wave> it = WAVES.iterator();
        while (it.hasNext()) {
            Wave wv = it.next();
            if (wv.owner.isRemoved() || wv.life-- <= 0) { it.remove(); continue; }
            ServerWorld w = wv.world;
            boolean dead = false;
            for (int step = 0; step < 2 && !dead; step++) {
                wv.pos = wv.pos.add(wv.dir.multiply(0.8));
                BlockPos bp = BlockPos.ofFloored(wv.pos);
                if (!w.getBlockState(bp).getCollisionShape(w, bp).isEmpty()) {
                    w.spawnParticles(ParticleTypes.EXPLOSION, wv.pos.x, wv.pos.y, wv.pos.z, 2, 0.3, 0.3, 0.3, 0);
                    w.playSound(null, bp, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.6f, 1.5f);
                    dead = true;
                    break;
                }
                for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, new Box(wv.pos, wv.pos).expand(1.4),
                        e -> e != wv.owner && e.isAlive() && !wv.hit.contains(e.getId()) && !isOwnedBy(e, wv.owner))) {
                    wv.hit.add(e.getId());
                    abilityDamage(e, wv.owner, WAVE_DMG[wv.stage]);
                    e.takeKnockback(1.2 + 0.4 * wv.stage, -wv.dir.x, -wv.dir.z);
                    e.velocityModified = true;
                }
                w.spawnParticles(ParticleTypes.SWEEP_ATTACK, wv.pos.x, wv.pos.y, wv.pos.z, 1, 0, 0, 0, 0);
                w.spawnParticles(ParticleTypes.CLOUD, wv.pos.x, wv.pos.y, wv.pos.z, 4, 0.35, 0.35, 0.35, 0.01);
                if (wv.stage >= 3) {
                    w.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, wv.pos.x, wv.pos.y, wv.pos.z, 3, 0.3, 0.3, 0.3, 0.01);
                }
            }
            if (dead) it.remove();
        }
    }

    // ================================================================== grab / throw

    private static void grab(ServerPlayerEntity p, RageData d) {
        if (d.grabbed != null) return;
        Entity t = raycastEntity(p, 5.0);
        if (t instanceof LivingEntity && !(t instanceof PlayerEntity)
                && !(t instanceof EnderDragonEntity) && !(t instanceof WitherEntity)) {
            d.grabbed = t;
            ServerWorld w = (ServerWorld) p.getWorld();
            w.playSound(null, t.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 1f, 0.6f);
            blood(w, t.getX(), t.getY() + t.getHeight() / 2, t.getZ(), 8);
        }
    }

    private static void tickGrab(ServerPlayerEntity p, RageData d) {
        Entity e = d.grabbed;
        if (e == null) return;
        if (!e.isAlive() || e.isRemoved() || e.getWorld() != p.getWorld() || e.squaredDistanceTo(p) > 144) {
            d.grabbed = null;
            return;
        }
        Vec3d look = p.getRotationVec(1f);
        Vec3d pos = p.getEyePos().add(look.multiply(2.2)).subtract(0, e.getHeight() / 2, 0);
        e.setVelocity(Vec3d.ZERO);
        e.fallDistance = 0;
        e.setPosition(pos.x, pos.y, pos.z);
        e.velocityModified = true;
    }

    private static void throwMob(ServerPlayerEntity p, RageData d) {
        Entity e = d.grabbed;
        if (e == null) return;
        d.grabbed = null;
        boolean big = !p.isOnGround();
        Vec3d look = p.getRotationVec(1f);
        double speed = big ? 3.2 : 2.4;
        e.setVelocity(look.multiply(speed).add(0, 0.2, 0));
        e.velocityModified = true;
        THROWN.add(new ThrownMob(e, p, big, d.stage));
        p.swingHand(Hand.MAIN_HAND, true);
        p.getWorld().playSound(null, p.getBlockPos(), SoundEvents.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 0.8f, 0.6f);
    }

    private static void tickThrownMobs() {
        Iterator<ThrownMob> it = THROWN.iterator();
        while (it.hasNext()) {
            ThrownMob tm = it.next();
            tm.age++;
            Entity e = tm.entity;
            if (e.isRemoved() || !(e.getWorld() instanceof ServerWorld w)) {
                it.remove();
                continue;
            }
            w.spawnParticles(BLOOD, e.getX(), e.getY() + e.getHeight() / 2, e.getZ(), 2, 0.2, 0.2, 0.2, 0);

            boolean hitEntity = !w.getOtherEntities(e, e.getBoundingBox().expand(0.4),
                    o -> o instanceof LivingEntity && o != tm.thrower && o.isAlive()).isEmpty();

            if ((tm.age > 3 && (e.isOnGround() || e.horizontalCollision || hitEntity || e.isTouchingWater()))
                    || tm.age > 80 || !e.isAlive()) {
                Vec3d pos = e.getPos();
                float mult = EXPLO_MULT[Math.max(1, tm.stage)];
                if (e.isAlive() && e instanceof LivingEntity le) abilityDamage(le, tm.thrower, (tm.big ? 20f : 12f) * mult);
                w.createExplosion(tm.thrower, pos.x, pos.y + 0.5, pos.z, (tm.big ? 4.5f : 2.5f) * mult, RageMod.EXPLOSION_TYPE);
                blood(w, pos.x, pos.y + 0.5, pos.z, tm.big ? 80 : 45);
                it.remove();
            }
        }
    }

    private static boolean isThrowableTool(ItemStack s) {
        return s.getItem() instanceof ToolItem || s.getItem() instanceof TridentItem;
    }

    private static void throwTool(ServerPlayerEntity p, RageData d) {
        if (d.throwCooldown > 0) return;
        ItemStack stack = p.getMainHandStack();
        if (!isThrowableTool(stack)) return;
        float dmg = stack.getItem() instanceof AxeItem ? 14f
                : (stack.getItem() instanceof SwordItem || stack.getItem() instanceof TridentItem) ? 12f : 9f;
        dmg *= EXPLO_MULT[d.stage];
        ItemStack thrown = stack.copy();
        if (!p.getAbilities().creativeMode) p.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);

        RageThrownItemEntity proj = new RageThrownItemEntity(p.getWorld(), p, thrown, false, dmg);
        Vec3d look = p.getRotationVec(1f);
        proj.setVelocity(look.x, look.y, look.z, 2.4f, 0.5f);
        p.getWorld().spawnEntity(proj);
        d.throwCooldown = 10;
        p.swingHand(Hand.MAIN_HAND, true);
        p.getWorld().playSound(null, p.getBlockPos(), SoundEvents.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1f, 0.8f);
    }

    private static void throwBlock(ServerPlayerEntity p, RageData d, int chargeTicks) {
        if (d.throwCooldown > 0) return;
        ItemStack stack = p.getMainHandStack();
        if (!(stack.getItem() instanceof BlockItem)) return;
        float charge = MathHelper.clamp(chargeTicks / 30f, 0f, 1f);
        ItemStack one = stack.copy();
        one.setCount(1);
        if (!p.getAbilities().creativeMode) stack.decrement(1);

        float power = (1.5f + 2.5f * charge) * EXPLO_MULT[d.stage];
        RageThrownItemEntity proj = new RageThrownItemEntity(p.getWorld(), p, one, true, power);
        Vec3d look = p.getRotationVec(1f);
        proj.setVelocity(look.x, look.y, look.z, 1.2f + 1.4f * charge, 1f);
        p.getWorld().spawnEntity(proj);
        d.throwCooldown = 6;
        p.swingHand(Hand.MAIN_HAND, true);
        // Wither kafatasi atis sesi
        p.getWorld().playSound(null, p.getBlockPos(), SoundEvents.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 1f, 0.9f);
    }

    // ================================================================== slam

    private static void slam(ServerPlayerEntity p, RageData d) {
        d.slamCooldown = 20;
        ServerWorld w = (ServerWorld) p.getWorld();
        Vec3d pos = p.getPos();
        w.createExplosion(p, pos.x, pos.y, pos.z, 3.0f * EXPLO_MULT[d.stage], RageMod.EXPLOSION_TYPE);

        LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(w);
        if (bolt != null) {
            bolt.refreshPositionAfterTeleport(pos);
            bolt.setCosmetic(true);
            w.spawnEntity(bolt);
        }
        for (int i = 0; i < 36; i++) {
            double a = i * (Math.PI * 2 / 36);
            w.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, w.getBlockState(p.getBlockPos().down())),
                    pos.x, pos.y + 0.1, pos.z, 0, Math.cos(a), 0.3, Math.sin(a), 0.6);
        }
        for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, p.getBoundingBox().expand(6),
                e -> e != p && e.isAlive() && !isOwnedBy(e, p))) {
            abilityDamage(e, p, 4f + 2f * d.stage);
            e.addVelocity(0, 0.9, 0);
            e.velocityModified = true;
        }
        shake(p, 3f);
    }

    // ================================================================== passive stuff

    private static void applySpeed(ServerPlayerEntity p, RageData d) {
        EntityAttributeInstance inst = p.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (inst == null) return;
        double target = SPRINT_BONUS[d.stage] * d.sprintRamp;
        if (d.stage >= 3 && !p.isSprinting()) target -= 0.3; // etap 3: agir agir yurur
        EntityAttributeModifier cur = inst.getModifier(SPEED_ID);
        if (cur != null && Math.abs(cur.getValue() - target) < 0.03) return;
        if (cur != null) inst.removeModifier(SPEED_ID);
        if (Math.abs(target) > 0.001) {
            inst.addTemporaryModifier(new EntityAttributeModifier(SPEED_ID, "rage_speed", target,
                    EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    private static void smashWalls(ServerPlayerEntity p, ServerWorld w, int stage) {
        Vec3d dir = Vec3d.fromPolar(0, p.getYaw());
        Box box = p.getBoundingBox().offset(dir.x * 0.8, 0, dir.z * 0.8).contract(0.05);
        if (stage >= 3) box = box.expand(0.5, 0, 0.5);
        BlockPos.Mutable m = new BlockPos.Mutable();
        boolean broke = false;
        for (int x = MathHelper.floor(box.minX); x <= MathHelper.floor(box.maxX); x++) {
            for (int y = MathHelper.floor(box.minY); y <= MathHelper.floor(box.maxY); y++) {
                for (int z = MathHelper.floor(box.minZ); z <= MathHelper.floor(box.maxZ); z++) {
                    m.set(x, y, z);
                    BlockState st = w.getBlockState(m);
                    if (st.isAir() || !st.getFluidState().isEmpty()) continue;
                    if (w.getBlockEntity(m) != null) continue;
                    float hardness = st.getHardness(w, m);
                    if (hardness < 0 || hardness > 30) continue;
                    if (st.getCollisionShape(w, m).isEmpty()) continue;
                    w.breakBlock(m, true, p);
                    broke = true;
                }
            }
        }
        if (broke) {
            w.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.PLAYERS, 0.8f, 0.8f);
            w.spawnParticles(ParticleTypes.EXPLOSION, p.getX() + dir.x, p.getY() + 1, p.getZ() + dir.z, 1, 0, 0, 0, 0);
        }
    }

    /** Etap 3: yurudukce yer yavasca ruh kumuna doner. */
    private static void tickSoulSand(ServerPlayerEntity p, RageData d, ServerWorld w) {
        if (!p.isOnGround() || p.age % 3 != 0) return;
        double moved = (p.getX() - d.lastX) * (p.getX() - d.lastX) + (p.getZ() - d.lastZ) * (p.getZ() - d.lastZ);
        if (moved < 0.0004) return;
        BlockPos under = BlockPos.ofFloored(p.getX(), p.getY() - 0.2, p.getZ());
        if (p.getRandom().nextFloat() < 0.5f) toSoulSand(w, under);
        if (p.getRandom().nextFloat() < 0.3f) {
            toSoulSand(w, under.add(p.getRandom().nextInt(5) - 2, 0, p.getRandom().nextInt(5) - 2));
        }
    }

    private static void toSoulSand(ServerWorld w, BlockPos pos) {
        BlockState st = w.getBlockState(pos);
        if (st.isAir() || st.isOf(Blocks.SOUL_SAND) || w.getBlockEntity(pos) != null) return;
        if (!st.isFullCube(w, pos)) return;
        float h = st.getHardness(w, pos);
        if (h < 0 || h > 50) return;
        w.setBlockState(pos, Blocks.SOUL_SAND.getDefaultState());
        w.spawnParticles(ParticleTypes.SOUL, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 2, 0.3, 0.05, 0.3, 0.01);
    }

    private static void scareMobs(ServerPlayerEntity p, RageData d, ServerWorld w) {
        int now = w.getServer().getTicks();
        double range = d.stage >= 3 ? 24 : 16;
        float brave = BRAVE[d.stage];
        for (PathAwareEntity mob : w.getEntitiesByClass(PathAwareEntity.class, p.getBoundingBox().expand(range),
                e -> e.isAlive() && e != d.grabbed && !isOwnedBy(e, p))) {
            Integer until = BRAVE_UNTIL.get(mob);
            if (until != null && until > now) continue;
            if (brave > 0 && mob.getRandom().nextFloat() < brave) {
                BRAVE_UNTIL.put(mob, now + 60);
                continue;
            }
            if (mob.getTarget() == p) mob.setTarget(null);
            if (mob.getNavigation().isIdle() || mob.squaredDistanceTo(p) < 49) {
                Vec3d away = NoPenaltyTargeting.findFrom(mob, 16, 7, p.getPos());
                if (away != null) mob.getNavigation().startMovingTo(away.x, away.y, away.z, d.stage >= 3 ? 1.6 : 1.4);
            }
        }
    }

    // ================================================================== hooks from mixin

    /** @return true ise hasar iptal edilir */
    public static boolean onDamage(LivingEntity self, DamageSource source, float amount) {
        Entity attacker = source.getAttacker();

        if (self instanceof ServerPlayerEntity p) {
            RageData d = get(p);
            if (d.active()) {
                if (source.isOf(DamageTypes.FALL)) return true;
                if (attacker != null && !(attacker instanceof PlayerEntity) && p.getRandom().nextFloat() < BLOCK_CHANCE[d.stage]) {
                    ServerWorld w = (ServerWorld) p.getWorld();
                    w.playSound(null, p.getBlockPos(), SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1f, 0.7f);
                    w.spawnParticles(ParticleTypes.ENCHANTED_HIT, p.getX(), p.getY() + 1, p.getZ(), 8, 0.4, 0.5, 0.4, 0.1);
                    return true;
                }
                if (amount > 0 && d.stage < 3) addNextBar(p, d, amount * DMG_FILL[d.stage]);
            } else if (amount > 0 && !p.isCreative() && !p.isSpectator() && !p.isInvulnerableTo(source)) {
                boolean wasFull = d.bar >= 100f;
                d.bar = Math.min(100f, d.bar + amount * 4f);
                d.ticksSinceDamaged = 0;
                if (!wasFull && d.bar >= 100f) {
                    p.sendMessage(Text.literal("SİNİR BARI DOLDU! [R] ile patla!").formatted(Formatting.RED, Formatting.BOLD), true);
                    p.getWorld().playSound(null, p.getBlockPos(), SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.PLAYERS, 1.5f, 1f);
                }
            }
        }

        // Rage'deki oyuncu biri(ler)ine vurdu
        if (attacker instanceof ServerPlayerEntity ap && ap != self) {
            RageData ad = get(ap);
            if (ad.active()) {
                ServerWorld w = (ServerWorld) self.getWorld();
                blood(w, self.getX(), self.getY() + self.getHeight() / 2, self.getZ(), 20);
                ad.ticksSinceHit = 0;
                ad.meter = Math.min(100f, ad.meter + 2.5f);
                if (ad.stage < 3) addNextBar(ap, ad, HIT_FILL[ad.stage]);

                // Hissedilen yumruk: sadece normal M1 vuruslarinda
                if (!abilityHit && source.isOf(DamageTypes.PLAYER_ATTACK) && source.getSource() == ap) {
                    punchFx(ap, ad, self, w);
                }
            }
        }
        return false;
    }

    /** Etap 3'te gelen hasarin azaltilmasi icin carpan. */
    public static float incomingFactor(LivingEntity self) {
        if (self instanceof ServerPlayerEntity p) {
            RageData d = get(p);
            if (d.active()) return DMG_TAKEN[d.stage];
        }
        return 1f;
    }

    private static void punchFx(ServerPlayerEntity p, RageData d, LivingEntity target, ServerWorld w) {
        int s = d.stage;
        Vec3d look = p.getRotationVec(1f);
        target.takeKnockback(PUNCH_KB[s], -look.x, -look.z);
        target.addVelocity(0, 0.1 * s, 0);
        target.velocityModified = true;

        double x = target.getX(), y = target.getY() + target.getHeight() / 2, z = target.getZ();
        w.playSound(null, target.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.2f, 0.6f);
        w.playSound(null, target.getBlockPos(), SoundEvents.ENTITY_IRON_GOLEM_ATTACK, SoundCategory.PLAYERS, 0.8f, 0.7f + 0.1f * s);
        w.spawnParticles(ParticleTypes.CRIT, x, y, z, 12, 0.3, 0.3, 0.3, 0.4);
        w.spawnParticles(ParticleTypes.SWEEP_ATTACK, x, y, z, 1, 0, 0, 0, 0);
        if (s >= 2) {
            w.playSound(null, target.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.4f, 1.8f);
            w.spawnParticles(ParticleTypes.EXPLOSION, x, y, z, 1, 0, 0, 0, 0);
        }
        if (s >= 3) w.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 10, 0.3, 0.3, 0.3, 0.08);
        shake(p, 0.8f + 0.5f * s);
    }

    private static void addNextBar(ServerPlayerEntity p, RageData d, float amount) {
        if (d.stage >= 3 || amount <= 0) return;
        boolean wasFull = d.bar >= 100f;
        d.bar = Math.min(100f, d.bar + amount);
        if (!wasFull && d.bar >= 100f) {
            p.sendMessage(Text.literal("DAHA DA SİNİRLENİYORSUN... [R] ile ETAP " + (d.stage + 1) + "!").formatted(Formatting.DARK_RED, Formatting.BOLD), true);
            p.getWorld().playSound(null, p.getBlockPos(), SoundEvents.ENTITY_WITHER_AMBIENT, SoundCategory.PLAYERS, 1.2f, 0.5f);
        }
    }

    public static void onDeath(LivingEntity self, DamageSource source) {
        if (!(self.getWorld() instanceof ServerWorld w)) return;

        if (source.getAttacker() instanceof ServerPlayerEntity killer && get(killer).active()) {
            RageData kd = get(killer);
            blood(w, self.getX(), self.getY() + self.getHeight() / 2, self.getZ(), 60);
            w.playSound(null, self.getBlockPos(), SoundEvents.ENTITY_SLIME_SQUISH, SoundCategory.HOSTILE, 1.2f, 0.6f);
            if (kd.stage < 3) addNextBar(killer, kd, KILL_FILL[kd.stage]);
        }

        if (self instanceof Tameable t && t.getOwnerUuid() != null && w.getServer() != null) {
            ServerPlayerEntity owner = w.getServer().getPlayerManager().getPlayer(t.getOwnerUuid());
            if (owner != null) {
                RageData d = get(owner);
                d.petRage = true;
                d.bar = 100f; // rage kapaliysa: rage hazir, aciksa: sonraki etap hazir
                if (d.active()) {
                    d.meter = 100f;
                    d.freezeTicks = Math.max(d.freezeTicks, FREEZE_PET);
                }
                owner.networkHandler.sendPacket(new TitleFadeS2CPacket(5, 40, 20));
                owner.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("ONU ÖLDÜRDÜLER").formatted(Formatting.DARK_RED, Formatting.BOLD)));
                owner.sendMessage(Text.literal("İntikam zamanı. [R] bas.").formatted(Formatting.RED), true);
                owner.getWorld().playSound(null, owner.getBlockPos(), SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.PLAYERS, 2f, 0.7f);
            }
        }
    }

    // ================================================================== helpers

    private static void abilityDamage(LivingEntity target, ServerPlayerEntity p, float amount) {
        abilityHit = true;
        try {
            target.damage(p.getDamageSources().playerAttack(p), amount);
        } finally {
            abilityHit = false;
        }
    }

    public static void blood(ServerWorld w, double x, double y, double z, int count) {
        w.spawnParticles(BLOOD, x, y, z, count, 0.3, 0.4, 0.3, 0.1);
        w.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.getDefaultState()),
                x, y, z, count / 2 + 1, 0.3, 0.3, 0.3, 0.15);
    }

    private static void title(ServerPlayerEntity p, String text, Formatting color) {
        p.networkHandler.sendPacket(new TitleFadeS2CPacket(3, 25, 10));
        p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal(text).formatted(color, Formatting.BOLD)));
    }

    private static void shake(ServerPlayerEntity p, float strength) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeFloat(strength);
        ServerPlayNetworking.send(p, RageNet.SHAKE, buf);
    }

    private static boolean isOwnedBy(Entity e, PlayerEntity p) {
        return e instanceof Tameable t && p.getUuid().equals(t.getOwnerUuid());
    }

    private static Entity raycastEntity(ServerPlayerEntity p, double range) {
        Vec3d start = p.getEyePos();
        Vec3d look = p.getRotationVec(1f);
        Vec3d end = start.add(look.multiply(range));
        Box box = p.getBoundingBox().stretch(look.multiply(range)).expand(1.0);
        EntityHitResult hit = ProjectileUtil.raycast(p, start, end, box,
                e -> !e.isSpectator() && e.canHit() && e != p, range * range);
        return hit == null ? null : hit.getEntity();
    }

    private static void addMod(ServerPlayerEntity p, EntityAttribute attr, UUID id, double value, EntityAttributeModifier.Operation op) {
        EntityAttributeInstance inst = p.getAttributeInstance(attr);
        if (inst == null) return;
        if (inst.getModifier(id) != null) inst.removeModifier(id);
        inst.addTemporaryModifier(new EntityAttributeModifier(id, "rage", value, op));
    }

    private static void removeMod(ServerPlayerEntity p, EntityAttribute attr, UUID id) {
        EntityAttributeInstance inst = p.getAttributeInstance(attr);
        if (inst != null && inst.getModifier(id) != null) inst.removeModifier(id);
    }

    private static void removeAllMods(ServerPlayerEntity p) {
        removeMod(p, EntityAttributes.GENERIC_MAX_HEALTH, HEALTH_ID);
        removeMod(p, EntityAttributes.GENERIC_ARMOR, ARMOR_ID);
        removeMod(p, EntityAttributes.GENERIC_ARMOR_TOUGHNESS, TOUGH_ID);
        removeMod(p, EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, KB_ID);
        removeMod(p, EntityAttributes.GENERIC_MOVEMENT_SPEED, SPEED_ID);
        removeMod(p, EntityAttributes.GENERIC_ATTACK_DAMAGE, ATTACK_ID);
    }

    private static void sync(ServerPlayerEntity p, RageData d) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(d.stage);
        buf.writeFloat(d.bar);
        buf.writeFloat(d.meter);
        buf.writeVarInt(d.freezeTicks);
        buf.writeVarInt(d.screamCooldown);
        buf.writeBoolean(d.grabbed != null);
        buf.writeBoolean(d.petRage);
        ServerPlayNetworking.send(p, RageNet.SYNC, buf);
    }
}
