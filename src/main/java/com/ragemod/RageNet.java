package com.ragemod;

import net.minecraft.util.Identifier;

public final class RageNet {
    public static final Identifier ACTION = new Identifier(RageMod.MOD_ID, "action");
    public static final Identifier SYNC = new Identifier(RageMod.MOD_ID, "sync");
    public static final Identifier SHAKE = new Identifier(RageMod.MOD_ID, "shake");

    public static final int A_SCREAM = 0;      // R: rage'i baslat / etap atla / bagir
    public static final int A_DEACTIVATE = 1;  // P
    public static final int A_KICK = 2;        // F
    public static final int A_GRAB = 3;        // M2 (canliya bakarken)
    public static final int A_THROW_MOB = 4;   // M2 (canli tutarken)
    public static final int A_THROW_TOOL = 5;  // M2 (elde kilic/balta/kazma)
    public static final int A_THROW_BLOCK = 6; // M2 basili tut + birak (elde blok)
    public static final int A_SUPERJUMP = 7;   // Shift + Space
    public static final int A_SLAM = 8;        // havada CTRL -> yere carpma
    public static final int A_DASH = 9;        // G
    public static final int A_PUNCH_WALL = 10; // etap 2+: Shift + M1 bloga
    public static final int A_AIR_WAVE = 11;   // etap 2+: havada M1 / yerde Shift + M1 bosluga

    private RageNet() {}
}
