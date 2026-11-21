package com.ragemod.client;

import com.ragemod.RageMod;
import com.ragemod.RageNet;
import com.ragemod.RageShared;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolItem;
import net.minecraft.item.TridentItem;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class RageModClient implements ClientModInitializer {

    // ================= TUSLAR (istersen degistir) =================
    public static final int KEY_SCREAM = GLFW.GLFW_KEY_R;     // rage baslat / etap atla / bagir
    public static final int KEY_CALM = GLFW.GLFW_KEY_P;       // rage kapat
    public static final int KEY_KICK = GLFW.GLFW_KEY_F;       // tekme
    public static final int KEY_DASH = GLFW.GLFW_KEY_G;       // dash
    // Sag tik = tut/firlat, Shift+Space = super ziplama, havada CTRL = yere carpma
    // Etap 2+: Shift+M1 bloga = duvar kir, havada M1 / yerde Shift+M1 bosluga = hava dalgasi
    // ===============================================================

    /** Etap 3 muzigi (kendi .ogg dosyani kaynak paketi ile ekleyebilirsin). */
    private static final Identifier STAGE3_MUSIC = new Identifier(RageMod.MOD_ID, "rage_stage3_music");

    private static final int[] KEYS = {KEY_SCREAM, KEY_CALM, KEY_KICK, KEY_DASH};
    private static final boolean[] prevKeys = new boolean[KEYS.length];

    private static boolean prevUse, prevAttack, prevJump, prevSprint, prevOnGround;
    private static boolean charging;
    private static int chargeTicks;
    private static boolean superAir, airJumpAvailable, slamming;
    private static int dashCooldown;
    private static SoundInstance music;

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(RageMod.THROWN_ITEM, FlyingItemEntityRenderer::new);

        ClientPlayNetworking.registerGlobalReceiver(RageNet.SYNC, (client, handler, buf, responseSender) -> {
            int stage = buf.readVarInt();
            float bar = buf.readFloat();
            float meter = buf.readFloat();
            int freeze = buf.readVarInt();
            int scream = buf.readVarInt();
            boolean grabbing = buf.readBoolean();
            boolean pet = buf.readBoolean();
            client.execute(() -> {
                ClientRageState.stage = stage;
                ClientRageState.active = stage > 0;
                ClientRageState.bar = bar;
                ClientRageState.meter = meter;
                ClientRageState.freezeTicks = freeze;
                ClientRageState.screamCooldown = scream;
                ClientRageState.grabbing = grabbing;
                ClientRageState.petRage = pet;
                RageShared.clientStage = stage;
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(RageNet.SHAKE, (client, handler, buf, responseSender) -> {
            float s = buf.readFloat();
            client.execute(() -> CameraShake.add(s));
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientRageState.reset();
            stopMusic(client);
        });

        HudRenderCallback.EVENT.register(RageHud::render);

        // Rage sirasinda F (el degistir) ve P (sosyal ekran) vanilla islevlerini yut
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player == null || !ClientRageState.active) return;
            while (client.options.swapHandsKey.wasPressed()) { /* yut */ }
            while (client.options.socialInteractionsKey.wasPressed()) { /* yut */ }
        });

        ClientTickEvents.END_CLIENT_TICK.register(RageModClient::onTick);
    }

    private static void send(int action, int arg) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(action);
        buf.writeVarInt(arg);
        ClientPlayNetworking.send(RageNet.ACTION, buf);
    }

    private static boolean isTool(ItemStack s) {
        return s.getItem() instanceof ToolItem || s.getItem() instanceof TridentItem;
    }

    public static boolean shouldBlockUse(MinecraftClient c) {
        if (!ClientRageState.active || c.player == null) return false;
        ItemStack main = c.player.getMainHandStack();
        return ClientRageState.grabbing
                || c.targetedEntity instanceof LivingEntity
                || main.getItem() instanceof BlockItem
                || isTool(main);
    }

    private static void onTick(MinecraftClient c) {
        CameraShake.tick();
        ClientPlayerEntity p = c.player;
        if (p == null) return;
        tickMusic(c);

        boolean noScreen = c.currentScreen == null;
        long win = c.getWindow().getHandle();

        boolean[] pressed = new boolean[KEYS.length];
        for (int i = 0; i < KEYS.length; i++) {
            boolean down = noScreen && InputUtil.isKeyPressed(win, KEYS[i]);
            pressed[i] = down && !prevKeys[i];
            prevKeys[i] = down;
        }

        boolean use = noScreen && c.options.useKey.isPressed();
        boolean attack = noScreen && c.options.attackKey.isPressed();
        boolean jump = noScreen && c.options.jumpKey.isPressed();
        boolean sprint = noScreen && c.options.sprintKey.isPressed();
        boolean sneak = noScreen && c.options.sneakKey.isPressed();
        boolean useEdge = use && !prevUse;
        boolean attackEdge = attack && !prevAttack;
        boolean jumpEdge = jump && !prevJump;
        boolean sprintEdge = sprint && !prevSprint;
        boolean onGround = p.isOnGround();

        if (dashCooldown > 0) dashCooldown--;

        if (pressed[0]) send(RageNet.A_SCREAM, 0);

        if (ClientRageState.active) {
            if (pressed[1]) send(RageNet.A_DEACTIVATE, 0);
            if (pressed[2]) send(RageNet.A_KICK, 0);
            if (pressed[3] && dashCooldown == 0) dash(p, onGround);

            // ---- M2: tut / firlat ----
            ItemStack main = p.getMainHandStack();
            if (useEdge && !charging) {
                if (ClientRageState.grabbing) send(RageNet.A_THROW_MOB, 0);
                else if (c.targetedEntity instanceof LivingEntity) send(RageNet.A_GRAB, 0);
                else if (isTool(main)) send(RageNet.A_THROW_TOOL, 0);
                else if (main.getItem() instanceof BlockItem) { charging = true; chargeTicks = 0; }
            }
            if (charging) {
                if (use) {
                    chargeTicks++;
                } else {
                    send(RageNet.A_THROW_BLOCK, chargeTicks);
                    charging = false;
                }
            }

            // ---- Etap 2+: M1 ozel saldirilari ----
            if (ClientRageState.stage >= 2 && attackEdge && c.targetedEntity == null) {
                boolean lookingAtBlock = c.crosshairTarget != null && c.crosshairTarget.getType() == HitResult.Type.BLOCK;
                if (!onGround) {
                    send(RageNet.A_AIR_WAVE, 0);               // havada M1: hava dalgasi
                } else if (sneak) {
                    send(lookingAtBlock ? RageNet.A_PUNCH_WALL // Shift+M1 bloga: duvar kir
                            : RageNet.A_AIR_WAVE, 0);          // Shift+M1 bosluga: hava dalgasi
                }
            }

            // ---- Ziplama yetenekleri ----
            if (onGround) {
                if (slamming) send(RageNet.A_SLAM, 0);
                slamming = false;
                superAir = false;
                airJumpAvailable = false;
            }

            Vec3d v = p.getVelocity();
            if (prevOnGround && jumpEdge && sneak) {
                p.setVelocity(v.x * 1.2, 1.25, v.z * 1.2);
                superAir = true;
                airJumpAvailable = true;
                send(RageNet.A_SUPERJUMP, 0);
            } else if (!onGround && superAir && jumpEdge && airJumpAvailable && !slamming) {
                p.setVelocity(v.x, 0.85, v.z);
                airJumpAvailable = false;
                send(RageNet.A_SUPERJUMP, 0);
            }

            if (superAir && !onGround && sprintEdge && !slamming) {
                p.setVelocity(v.x * 0.2, -3.0, v.z * 0.2);
                slamming = true;
            }
        } else {
            charging = false;
            superAir = false;
            slamming = false;
        }

        prevUse = use;
        prevAttack = attack;
        prevJump = jump;
        prevSprint = sprint;
        prevOnGround = onGround;
    }

    private static void tickMusic(MinecraftClient c) {
        if (ClientRageState.stage >= 3) {
            if (music == null) {
                c.getMusicTracker().stop();
                music = new PositionedSoundInstance(STAGE3_MUSIC, SoundCategory.RECORDS, 1.0f, 1.0f,
                        SoundInstance.createRandom(), true, 0, SoundInstance.AttenuationType.NONE, 0, 0, 0, true);
                c.getSoundManager().play(music);
            }
            if (c.player != null && c.player.age % 200 == 0) c.getMusicTracker().stop();
        } else if (music != null) {
            stopMusic(c);
        }
    }

    private static void stopMusic(MinecraftClient c) {
        if (music != null) {
            c.getSoundManager().stop(music);
            music = null;
        }
    }

    private static void dash(ClientPlayerEntity p, boolean onGround) {
        float fwd = p.input.movementForward;
        float side = p.input.movementSideways;
        double yaw = Math.toRadians(p.getYaw());
        double sin = Math.sin(yaw), cos = Math.cos(yaw);
        Vec3d dir;
        if (Math.abs(fwd) + Math.abs(side) < 0.01f) {
            dir = new Vec3d(-sin, 0, cos);
        } else {
            dir = new Vec3d(side * cos - fwd * sin, 0, fwd * cos + side * sin).normalize();
        }
        double strength = onGround ? 1.9 : 1.1;
        Vec3d v = p.getVelocity();
        p.setVelocity(dir.x * strength, onGround ? 0.25 : Math.max(v.y, 0.15), dir.z * strength);
        dashCooldown = 25;
        send(RageNet.A_DASH, 0);
    }
}
