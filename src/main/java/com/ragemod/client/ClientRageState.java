package com.ragemod.client;

import com.ragemod.RageShared;

/** Sunucudan gelen sinir bilgisi (arayuz icin). */
public final class ClientRageState {
    public static int stage = 0;
    public static boolean active = false;
    public static float bar = 0f;
    public static float meter = 0f;
    public static int freezeTicks = 0;
    public static int screamCooldown = 0;
    public static boolean grabbing = false;
    public static boolean petRage = false;

    public static void reset() {
        stage = 0; active = false; bar = 0; meter = 0; freezeTicks = 0; screamCooldown = 0; grabbing = false; petRage = false;
        RageShared.clientStage = 0;
    }

    private ClientRageState() {}
}
