package com.ragemod.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

/** Guclu vuruslarda ekran sarsintisi. */
public final class CameraShake {
    private CameraShake() {}

    private static float intensity = 0f;

    public static void add(float strength) {
        intensity = Math.min(6f, Math.max(intensity, strength));
    }

    public static void tick() {
        intensity *= 0.75f;
        if (intensity < 0.02f) intensity = 0f;
    }

    public static void apply(MatrixStack matrices, float tickDelta) {
        if (intensity <= 0f) return;
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.world == null) return;
        float t = c.world.getTime() + tickDelta;
        float roll = (float) Math.sin(t * 2.7) * intensity;
        float pitch = (float) Math.cos(t * 3.3) * intensity * 0.6f;
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(roll));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
    }
}
