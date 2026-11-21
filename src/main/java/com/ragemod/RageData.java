package com.ragemod;

import net.minecraft.entity.Entity;

import java.util.HashSet;
import java.util.Set;

/** Bir oyuncunun sinir durumu (sadece sunucu tarafinda tutulur). */
public class RageData {
    /** 0 = rage kapali, 1/2/3 = rage etabi. */
    public int stage = 0;
    /**
     * Sinir bari (0-100).
     * Rage kapaliyken: dolunca R ile rage 1 baslar.
     * Rage acikken: bir sonraki etabin bari (daha zor dolar).
     */
    public float bar = 0f;
    /** Rage metresi (0-100). Sifirlanirsa rage biter. */
    public float meter = 0f;
    /** Metrenin hic azalmayacagi kalan sure (tick, 20 tick = 1 sn). */
    public int freezeTicks = 0;
    /** Evcil hayvan oldu mu? (rage acilinca 6 dk kilit). */
    public boolean petRage = false;

    public int ticksSinceHit = 9999;
    public int ticksSinceDamaged = 9999;

    public int screamCooldown = 0;
    public int kickCooldown = 0;
    public int throwCooldown = 0;
    public int slamCooldown = 0;
    public int smashCooldown = 0;
    public int waveCooldown = 0;

    public int dashTicks = 0;
    public final Set<Integer> dashHit = new HashSet<>();

    public float sprintRamp = 0f;
    public Entity grabbed = null;

    public double lastX, lastZ;

    public boolean active() { return stage > 0; }
}
