package com.ragemod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class RageMod implements ModInitializer {
    public static final String MOD_ID = "ragemod";

    // ================= AYARLAR (istersen degistir) =================
    /** Patlamalar blok kirsin mi? NONE = kirmaz (evini korur). TNT = kirar. */
    public static final World.ExplosionSourceType EXPLOSION_TYPE = World.ExplosionSourceType.NONE;
    // ================================================================

    public static EntityType<RageThrownItemEntity> THROWN_ITEM;

    @Override
    public void onInitialize() {
        THROWN_ITEM = Registry.register(
                Registries.ENTITY_TYPE,
                new Identifier(MOD_ID, "thrown_item"),
                FabricEntityTypeBuilder.<RageThrownItemEntity>create(SpawnGroup.MISC, RageThrownItemEntity::new)
                        .dimensions(EntityDimensions.fixed(0.3f, 0.3f))
                        .trackRangeBlocks(64)
                        .trackedUpdateRate(10)
                        .build());

        ServerPlayNetworking.registerGlobalReceiver(RageNet.ACTION, (server, player, handler, buf, responseSender) -> {
            int action = buf.readVarInt();
            int arg = buf.readVarInt();
            server.execute(() -> RageManager.handleAction(player, action, arg));
        });

        ServerTickEvents.END_SERVER_TICK.register(RageManager::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> RageManager.reset(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> RageManager.reset(handler.player));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> RageManager.reset(newPlayer));
    }
}
