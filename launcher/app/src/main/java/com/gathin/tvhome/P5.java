package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.PixelFormat;

import java.util.Locale;
import java.util.Random;

/**
 * Shared pieces of the Persona 5 style: palette, fonts, vector shapes and the procedural "ransom note" lettering.
 * Everything is drawn in code (no game assets); the fonts are open-licence substitutes (res/font, licences in
 * launcher/fonts-license): Anton and Bebas Neue for the heavy condensed sans, Alfa Slab One and Abril Fatface
 * for the slab and display serif letters.
 */
final class P5 {
    static final int RED = 0xFFE60012;
    static final int DARK_RED = 0xFF8F000C;
    static final int BLACK = 0xFF0A0A0A;
    static final int WHITE = 0xFFFFFFFF;
    static final int CREAM = 0xFFF2E9D8;

    static Typeface anton, bebas, slab, serif;

    private P5() {
    }

    static void init(Context c) {
        if (anton != null) return;
        android.content.res.Resources r = c.getResources();
        anton = r.getFont(R.font.p5_anton);
        bebas = r.getFont(R.font.p5_bebas);
        slab = r.getFont(R.font.p5_slab);
        serif = r.getFont(R.font.p5_serif);
    }

    static Paint paint(int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        return p;
    }

    /** Closed polygon from x,y pairs. */
    static Path poly(float... xy) {
        Path p = new Path();
        p.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) p.lineTo(xy[i], xy[i + 1]);
        p.close();
        return p;
    }

    /** A five pointed star centred on cx,cy with outer radius r, point up, rotated by deg. */
    static Path star(float cx, float cy, float r, float deg) {
        Path p = new Path();
        float inner = r * 0.40f;
        for (int i = 0; i < 10; i++) {
            double a = Math.toRadians(-90 + deg + i * 36);
            float rad = i % 2 == 0 ? r : inner;
            float x = cx + (float) Math.cos(a) * rad, y = cy + (float) Math.sin(a) * rad;
            if (i == 0) p.moveTo(x, y);
            else p.lineTo(x, y);
        }
        p.close();
        return p;
    }

    /**
     * The jagged highlight shard behind the focused item: a wide, slightly torn parallelogram. The vertex list is
     * normalised so one shape serves every tile size.
     */
    private static final float[] SHARD = {
            0.05f, 0.14f, 0.20f, 0.03f, 0.27f, 0.12f, 0.46f, 0.00f, 0.53f, 0.09f, 0.74f, 0.02f, 0.80f, 0.10f, 1.00f, 0.00f,
            0.95f, 0.86f, 0.82f, 0.93f, 0.76f, 0.85f, 0.58f, 1.00f, 0.51f, 0.91f, 0.33f, 0.99f, 0.27f, 0.90f, 0.08f, 1.00f,
            0.00f, 0.52f};

    static Path shard(float l, float t, float r, float b, float scale) {
        float w = r - l, h = b - t, cx = (l + r) / 2f, cy = (t + b) / 2f;
        Path p = new Path();
        for (int i = 0; i < SHARD.length; i += 2) {
            float x = cx + (l + SHARD[i] * w - cx) * scale, y = cy + (t + SHARD[i + 1] * h - cy) * scale;
            if (i == 0) p.moveTo(x, y);
            else p.lineTo(x, y);
        }
        p.close();
        return p;
    }

    /** Halftone dots over a rect: radius grows from 0 at the "from" edge (top, if fromTop) to maxR at the opposite edge. */
    static void halftone(Canvas c, Paint paint, float l, float t, float r, float b, float step, float maxR, boolean growDown) {
        int row = 0;
        for (float y = t; y <= b; y += step * 0.866f, row++) {
            float f = (y - t) / Math.max(1f, b - t);
            if (!growDown) f = 1f - f;
            float rad = maxR * f;
            if (rad < 0.8f) continue;
            float off = (row & 1) == 0 ? 0 : step / 2f;
            for (float x = l + off; x <= r; x += step) c.drawCircle(x, y, rad, paint);
        }
    }

    // ---- ransom note ---------------------------------------------------------------------------

    private static final int[][] LETTER_STYLES = {
            {WHITE, BLACK}, {BLACK, WHITE}, {RED, WHITE}, {CREAM, BLACK}, {WHITE, RED}, {BLACK, WHITE}};

    /**
     * Ransom-note lettering: every letter in its own cut-out box with a different font, size, colour pair and tilt.
     * Deterministic for a given text and seed, so a title looks the same each time it appears.
     *
     * @param basePx letter size in px
     */
    static Bitmap ransom(String text, float basePx, long seed) {
        String s = text.toUpperCase(Locale.ENGLISH);
        Random rnd = new Random(seed * 31 + s.hashCode());
        Typeface[] faces = {anton, slab, serif, bebas, anton, slab};
        int n = s.length();
        float[] size = new float[n], boxW = new float[n], rot = new float[n], dy = new float[n];
        int[] style = new int[n];
        Typeface[] face = new Typeface[n];
        Paint m = new Paint(Paint.ANTI_ALIAS_FLAG);
        float total = basePx * 0.5f;
        for (int i = 0; i < n; i++) {
            size[i] = basePx * (0.80f + rnd.nextFloat() * 0.40f);
            face[i] = faces[rnd.nextInt(faces.length)];
            style[i] = rnd.nextInt(LETTER_STYLES.length);
            // Never two identical colour pairs in a row, or the note looks like one word again.
            if (i > 0 && style[i] == style[i - 1]) style[i] = (style[i] + 1) % LETTER_STYLES.length;
            rot[i] = (rnd.nextFloat() - 0.5f) * 15f;
            dy[i] = (rnd.nextFloat() - 0.5f) * basePx * 0.16f;
            m.setTypeface(face[i]);
            m.setTextSize(size[i]);
            char ch = s.charAt(i);
            boxW[i] = ch == ' ' ? basePx * 0.35f : Math.max(size[i] * 0.62f, m.measureText(String.valueOf(ch)) + size[i] * 0.34f);
            total += boxW[i] * (ch == ' ' ? 1f : 0.93f);
        }
        float h = basePx * 1.18f;
        int bw = (int) (total + basePx * 0.5f), bh = (int) (h * 1.45f);
        Bitmap bmp = Bitmap.createBitmap(Math.max(1, bw), Math.max(1, bh), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Paint box = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadow.setColor(0xFF000000);
        Paint txt = new Paint(Paint.ANTI_ALIAS_FLAG);
        Rect tb = new Rect();
        float x = basePx * 0.25f, cy = bh / 2f;
        float sh = basePx * 0.07f;
        for (int i = 0; i < n; i++) {
            char ch = s.charAt(i);
            float w = boxW[i];
            if (ch == ' ') {
                x += w;
                continue;
            }
            float cx = x + w / 2f;
            c.save();
            c.translate(cx, cy + dy[i]);
            c.rotate(rot[i]);
            float bh2 = h / 2f * (0.92f + rnd.nextFloat() * 0.14f);
            float j = basePx * 0.05f;
            // Slightly cut, uneven corners, like scissored paper.
            float[] q = {-w / 2 + rnd.nextFloat() * j, -bh2 + rnd.nextFloat() * j, w / 2 - rnd.nextFloat() * j, -bh2 + rnd.nextFloat() * j,
                    w / 2 - rnd.nextFloat() * j, bh2 - rnd.nextFloat() * j, -w / 2 + rnd.nextFloat() * j, bh2 - rnd.nextFloat() * j};
            Path shape = poly(q);
            c.save();
            c.translate(sh, sh);
            c.drawPath(shape, shadow);
            c.restore();
            box.setColor(LETTER_STYLES[style[i]][0]);
            c.drawPath(shape, box);
            txt.setTypeface(face[i]);
            txt.setTextSize(size[i]);
            txt.setColor(LETTER_STYLES[style[i]][1]);
            String g = String.valueOf(ch);
            txt.getTextBounds(g, 0, 1, tb);
            c.drawText(g, -txt.measureText(g) / 2f, -(tb.top + tb.bottom) / 2f, txt);
            c.restore();
            x += w * 0.93f;
        }
        return bmp;
    }

    // ---- stateful tag / row / panel background -------------------------------------------------

    /** Black slanted slab that turns red with a white edge and a hard shadow when focused. */
    static final class Tag extends Drawable {
        static final int TAG = 0, ROW = 1, PANEL = 2, DOCK = 3;
        private final int mode;
        private final float d;
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private boolean focused;

        Tag(Context ctx, int mode) {
            this.mode = mode;
            this.d = ctx.getResources().getDisplayMetrics().density;
        }

        @Override
        public boolean isStateful() {
            return mode != PANEL;
        }

        @Override
        protected boolean onStateChange(int[] state) {
            boolean f = false;
            for (int s : state) if (s == android.R.attr.state_focused) f = true;
            if (f == focused) return false;
            focused = f;
            invalidateSelf();
            return true;
        }

        @Override
        public void draw(Canvas c) {
            Rect b = getBounds();
            float sk = (mode == TAG ? 10f : mode == ROW ? 8f : 14f) * d;
            float sh = (mode == TAG ? 5f : 0f) * d;
            float l = b.left, t = b.top, r = b.right - sh, bo = b.bottom - sh;
            if (mode == DOCK) {
                // Idle dock item: a slim black slanted plate; the focused one is drawn by the red shard instead.
                if (focused) return;
                float cy = (t + bo) / 2f, hh = 31f * d;
                float tt = cy - hh, bb = cy + hh;
                fill.setColor(0xF2080808);
                c.drawPath(poly(l + sk, tt, r - 4 * d, tt, r - sk - 4 * d, bb, l + 4 * d, bb), fill);
                fill.setColor(RED);
                c.drawPath(poly(l + sk, tt, l + sk + 6 * d, tt, l + 10 * d, bb, l + 4 * d, bb), fill);
                fill.setColor(0x80FFFFFF);
                c.drawPath(poly(l + 4 * d, bb - 2f * d, r - sk - 4 * d, bb - 2f * d, r - sk - 5 * d, bb, l + 4 * d, bb), fill);
                return;
            }
            if (mode == PANEL) {
                fill.setColor(0xF2080808);
                c.drawPath(poly(l + sk, t, r, t, r - sk, bo, l, bo), fill);
                fill.setColor(RED);
                c.drawPath(poly(l + sk, t, l + sk + 8 * d, t, l + 8 * d, bo, l, bo), fill);
                fill.setStyle(Paint.Style.STROKE);
                fill.setStrokeWidth(2f * d);
                fill.setColor(WHITE);
                c.drawPath(poly(l + sk, t + d, r - d, t + d, r - sk - d, bo - d, l + d, bo - d), fill);
                fill.setStyle(Paint.Style.FILL);
                return;
            }
            if (focused) {
                if (sh > 0) {
                    fill.setColor(0xFF000000);
                    c.drawPath(poly(l + sk + sh, t + sh, r + sh, t + sh, r - sk + sh, bo + sh, l + sh, bo + sh), fill);
                }
                fill.setColor(WHITE);
                c.drawPath(poly(l + sk, t, r, t, r - sk, bo, l, bo), fill);
                fill.setColor(RED);
                float i = 3f * d;
                c.drawPath(poly(l + sk + i * 1.4f, t + i, r - i, t + i, r - sk - i * 1.4f, bo - i, l + i, bo - i), fill);
            } else {
                if (mode == TAG) {
                    fill.setColor(0xFF000000);
                    c.drawPath(poly(l + sk + sh, t + sh, r + sh, t + sh, r - sk + sh, bo + sh, l + sh, bo + sh), fill);
                }
                fill.setColor(mode == TAG ? 0xF50A0A0A : 0x00000000);
                c.drawPath(poly(l + sk, t, r, t, r - sk, bo, l, bo), fill);
                if (mode == TAG) {
                    fill.setColor(RED);
                    c.drawPath(poly(l + sk, t, l + sk + 7 * d, t, l + 7 * d, bo, l, bo), fill);
                }
            }
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(android.graphics.ColorFilter cf) {
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    static RectF rect(float l, float t, float r, float b) {
        return new RectF(l, t, r, b);
    }
}
