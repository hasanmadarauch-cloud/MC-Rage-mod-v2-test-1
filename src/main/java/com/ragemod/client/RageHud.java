package com.ragemod.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

import java.util.Random;

/** Sinir bari, kirmizi ekran ve sagdaki RAGE metresi. */
public final class RageHud {
    private RageHud() {}

    public static void render(DrawContext ctx, float tickDelta) {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.player == null || c.world == null || c.options.hudHidden) return;
        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();
        float t = c.world.getTime() + tickDelta;
        int stage = ClientRageState.stage;

        if (stage > 0) {
            drawRedScreen(ctx, w, h, t, stage);
            drawMeter(ctx, c.textRenderer, w, h, t, stage);
            drawControls(ctx, c.textRenderer, h, stage);
            if (stage < 3) drawBar(ctx, c.textRenderer, w, h, t, stage);
        } else {
            drawBar(ctx, c.textRenderer, w, h, t, 0);
        }
    }

    private static int rainbow(float hue) {
        return 0xFF000000 | MathHelper.hsvToRgb(((hue % 1f) + 1f) % 1f, 0.85f, 1f);
    }

    // ---------------- kucuk sinir bari (hotbar'in saginda)
    private static void drawBar(DrawContext ctx, TextRenderer tr, int w, int h, float t, int stage) {
        int bw = 70, bh = 8;
        int x = w / 2 + 96;
        int y = h - 14;
        float pct = Math.min(1f, ClientRageState.bar / 100f);
        boolean full = pct >= 1f;
        boolean nextStage = stage > 0; // rage acikken bu bar bir sonraki etap icin

        ctx.fill(x - 2, y - 2, x + bw + 2, y + bh + 2, 0xFF1A0612);
        int frame = full ? rainbow(t * 0.03f) : 0xFF4B2A8C;
        ctx.fill(x - 1, y - 1, x + bw + 1, y + bh + 1, frame);
        ctx.fill(x, y, x + bw, y + bh, 0xFF22101A);
        int fw = (int) (bw * pct);
        if (fw > 0) {
            if (full && nextStage) {
                // gokkusagi bar
                for (int i = 0; i < fw; i++) {
                    ctx.fill(x + i, y, x + i + 1, y + bh, rainbow(i / 70f + t * 0.03f));
                }
            } else if (nextStage) {
                ctx.fillGradient(x, y, x + fw, y + bh, 0xFFFFD040, 0xFFB03000);
            } else {
                ctx.fillGradient(x, y, x + fw, y + bh, 0xFFFF5040, 0xFF8A0000);
            }
        }
        for (int i = 1; i < 5; i++) ctx.fill(x + bw * i / 5, y, x + bw * i / 5 + 1, y + 2, 0xFF6FD3FF);

        String label;
        int color;
        if (nextStage) {
            label = "ETAP " + (stage + 1);
            color = full ? rainbow(t * 0.05f) : 0xFFFFB040;
        } else if (ClientRageState.petRage) {
            label = "İNTİKAM";
            color = 0xFFB00000;
        } else {
            label = "RAGE";
            color = 0xFFFF4040;
        }
        ctx.drawTextWithShadow(tr, label, x, y - 11, color);
        if (full) {
            String hint = "[R]";
            int hx = x + bw - tr.getWidth(hint);
            ctx.drawTextWithShadow(tr, hint, hx, y - 11, ((int) (t / 5) % 2 == 0) ? 0xFFFFFFFF : 0xFFFF3030);
        }
    }

    // ---------------- rage: renkli ekran
    private static void drawRedScreen(DrawContext ctx, int w, int h, float t, int stage) {
        int rgb = stage >= 3 ? 0x2A0010 : 0xFF0000;
        int edge = stage >= 3 ? 0x101018 : 0xAA0000;
        int base = stage >= 3 ? 40 : 20 + stage * 8;
        int pulse = (int) (base + 14 * Math.sin(t * (0.1 + 0.05 * stage)));
        ctx.fill(0, 0, w, h, (pulse << 24) | rgb);
        int edgeA = stage >= 3 ? 0xCC : 0x99;
        ctx.fillGradient(0, 0, w, h / 4, (edgeA << 24) | edge, edge);
        ctx.fillGradient(0, h * 3 / 4, w, h, edge, (edgeA << 24) | edge);
        for (int i = 0; i < 24; i++) {
            int a = (int) (0x80 * (1f - i / 24f));
            int col = (a << 24) | edge;
            ctx.fill(i * 2, 0, i * 2 + 2, h, col);
            ctx.fill(w - i * 2 - 2, 0, w - i * 2, h, col);
        }
    }

    // ---------------- rage: sagdaki dikey metre
    private static void drawMeter(DrawContext ctx, TextRenderer tr, int w, int h, float t, int stage) {
        int mw = 22, mh = 120;
        int mx = w - mw - 18;
        int my = h / 2 - mh / 2 + 10;
        Random rnd = new Random((long) (t / 2));

        int bolt = stage >= 3 ? 0xCC40FFF0 : stage == 2 ? 0xCCFFD040 : 0xCC6FD3FF;
        for (int side = 0; side < 2; side++) {
            int bx = side == 0 ? mx - 6 : mx + mw + 3;
            int yy = my + rnd.nextInt(10);
            while (yy < my + mh) {
                int len = 4 + rnd.nextInt(6);
                int off = rnd.nextInt(5) - 2 - (stage - 1);
                if (rnd.nextFloat() < 0.6f + 0.1f * stage) ctx.fill(bx + off, yy, bx + off + 2, yy + len, bolt);
                yy += len + rnd.nextInt(5);
            }
        }

        ctx.fill(mx - 2, my - 2, mx + mw + 2, my + mh + 2, 0xFF120818);
        if (stage >= 3) ctx.fillGradient(mx, my, mx + mw, my + mh, 0xFF0A5A60, 0xFF101030);
        else ctx.fillGradient(mx, my, mx + mw, my + mh, 0xFF3A2AA8, 0xFF7A1FB0);
        int ix = mx + 4, iy = my + 4, iw = mw - 8, ih = mh - 8;
        ctx.fill(ix, iy, ix + iw, iy + ih, 0xFF1A0A10);

        float pct = Math.max(0f, Math.min(1f, ClientRageState.meter / 100f));
        int fh = (int) (ih * pct);
        if (fh > 0) {
            if (stage >= 3) ctx.fillGradient(ix, iy + ih - fh, ix + iw, iy + ih, 0xFF60FFF0, 0xFF0A6070);
            else ctx.fillGradient(ix, iy + ih - fh, ix + iw, iy + ih, 0xFFFF3A3A, 0xFFB00000);
        }
        if (fh > 2) ctx.fill(ix, iy + ih - fh, ix + iw, iy + ih - fh + 2, 0xFF6FD3FF);

        for (int i = 1; i < 6; i++) {
            int ty = iy + ih * i / 6;
            ctx.fill(ix, ty, ix + 5, ty + 1, 0xFF6FD3FF);
        }

        String title = stage == 3 ? "RAGE III" : stage == 2 ? "RAGE II" : "RAGE";
        int titleCol = stage >= 3 ? 0xFF40FFF0 : 0xFFFF2020;
        float s = 1.6f;
        int shakeAmt = stage;
        int jx = rnd.nextInt(2 * shakeAmt + 1) - shakeAmt, jy = rnd.nextInt(2 * shakeAmt + 1) - shakeAmt;
        ctx.getMatrices().push();
        ctx.getMatrices().translate(mx + mw / 2f + jx, my - 18 + jy, 0);
        ctx.getMatrices().scale(s, s, 1f);
        ctx.drawText(tr, title, -tr.getWidth(title) / 2, 0, titleCol, true);
        ctx.getMatrices().pop();

        String info;
        int infoCol;
        if (ClientRageState.freezeTicks > 0) {
            int sec = ClientRageState.freezeTicks / 20;
            info = String.format("%d:%02d", sec / 60, sec % 60);
            infoCol = 0xFF6FD3FF;
        } else {
            info = (int) ClientRageState.meter + "%";
            infoCol = 0xFFFF6060;
        }
        ctx.drawTextWithShadow(tr, info, mx + mw / 2 - tr.getWidth(info) / 2, my + mh + 5, infoCol);
    }

    // ---------------- rage: sol taraftaki kontrol listesi
    private static void drawControls(DrawContext ctx, TextRenderer tr, int h, int stage) {
        int x = 10;
        int y = h / 2 - 50;
        int cd = ClientRageState.screamCooldown;
        String scream = "SCREAM/R" + (cd > 0 ? "  " + (cd / 20 + 1) + "s" : "");
        ctx.drawTextWithShadow(tr, scream, x, y, cd > 0 ? 0xFF888888 : 0xFFFFFFFF);
        y += 11;
        ctx.drawTextWithShadow(tr, "GRAB/M2" + (ClientRageState.grabbing ? "  [TUTUYOR]" : ""), x, y, 0xFFFFFFFF);
        y += 11;
        ctx.drawTextWithShadow(tr, "KICK/F", x, y, 0xFFFFFFFF);
        y += 11;
        ctx.drawTextWithShadow(tr, "DASH/G" + (stage >= 3 ? "  (HASAR)" : ""), x, y, 0xFFFFFFFF);
        y += 11;
        ctx.drawTextWithShadow(tr, "SHIFT+SPACE: SÜPER ZIPLA", x, y, 0xFFDDDDDD);
        y += 11;
        ctx.drawTextWithShadow(tr, "HAVADA CTRL: YERE ÇAK", x, y, 0xFFDDDDDD);
        y += 11;
        if (stage >= 2) {
            ctx.drawTextWithShadow(tr, "SHIFT+M1 BLOĞA: DUVAR KIR", x, y, 0xFFFFC040);
            y += 11;
            ctx.drawTextWithShadow(tr, "HAVADA M1: HAVA DALGASI", x, y, 0xFFFFC040);
            y += 11;
        }
        ctx.drawTextWithShadow(tr, "P: SAKİNLEŞ", x, y, 0xFFAAAAAA);
    }
}
