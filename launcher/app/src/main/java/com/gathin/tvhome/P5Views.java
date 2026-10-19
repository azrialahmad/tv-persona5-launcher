package com.gathin.tvhome;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * The custom-drawn views of the Persona 5 style. All vector drawing in code; static things are drawn once and the
 * GPU just replays them, so only focus changes cost anything.
 */
final class P5Views {
    private P5Views() {
    }

    static String timeOfDay(int hour) {
        if (hour < 5) return "NIGHT";
        if (hour < 12) return "MORNING";
        if (hour < 17) return "AFTERNOON";
        if (hour < 22) return "EVENING";
        return "NIGHT";
    }

    // ---- backdrop ------------------------------------------------------------------------------

    /** Dark diagonal slabs, red edges and halftone dots over the wallpaper: the calendar corner and the menu band. */
    static final class Backdrop extends View {
        private final float d;
        private final Paint paint = P5.paint(0);
        private Path band, bandStripe, bandStripe2, corner, cornerStripe, cornerStripe2;
        private float dotL, dotT, dotR, dotB, cDotL, cDotT, cDotR, cDotB;

        private final boolean bandMode;

        Backdrop(Context c, boolean bandMode) {
            super(c);
            d = c.getResources().getDisplayMetrics().density;
            this.bandMode = bandMode;
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        @Override
        protected void onSizeChanged(int w, int h, int ow, int oh) {
            float u = bandMode ? h - 330 * d : h;
            float tl = 0.575f * u, tr = 0.505f * u;   // band top edge, left and right
            band = P5.poly(0, tl, w, tr, w, h, 0, h);
            bandStripe = P5.poly(0, tl - 20 * d, w, tr - 20 * d, w, tr, 0, tl);
            bandStripe2 = P5.poly(0, tl + 8 * d, w, tr + 8 * d, w, tr + 12 * d, 0, tl + 12 * d);
            dotL = 0;
            dotR = w;
            dotT = tl - 150 * d;
            dotB = tl - 28 * d;
            corner = P5.poly(0, 0, 0.455f * w, 0, 0.385f * w, 0.265f * h, 0, 0.30f * h);
            cornerStripe = P5.poly(0.455f * w, 0, 0.455f * w + 16 * d, 0, 0.385f * w + 16 * d, 0.265f * h, 0.385f * w, 0.265f * h);
            cornerStripe2 = P5.poly(0, 0.30f * h, 0.385f * w, 0.265f * h, 0.385f * w + 16 * d, 0.265f * h, 0, 0.30f * h + 14 * d);
            cDotL = 0.40f * w;
            cDotR = 0.55f * w;
            cDotT = 0;
            cDotB = 0.27f * h;
        }

        @Override
        protected void onDraw(Canvas c) {
            int w = getWidth(), h = getHeight();
            if (!bandMode) {
                paint.setColor(0x52000000);
                c.drawRect(0, 0, w, h, paint);
            }
            if (bandMode) {
            paint.setColor(0x66E60012);
            P5.halftone(c, paint, dotL, dotT, dotR, dotB, 14 * d, 4.2f * d, true);
            paint.setColor(0xEC070707);
            c.drawPath(band, paint);
            paint.setColor(P5.RED);
            c.drawPath(bandStripe, paint);
            paint.setColor(P5.WHITE);
            c.drawPath(bandStripe2, paint);
            return;
            }
            // Calendar corner.
            paint.setColor(0x66E60012);
            P5.halftone(c, paint, cDotL, cDotT, cDotR, cDotB, 13 * d, 3.6f * d, false);
            paint.setColor(0xEC070707);
            c.drawPath(corner, paint);
            paint.setColor(P5.RED);
            c.drawPath(cornerStripe, paint);
        }
    }

    // ---- calendar corner -----------------------------------------------------------------------

    /** Date, weekday and time of day, laid out like Persona 5's calendar. Redraws on the minute. */
    static final class CalendarCorner extends View {
        private final float d;
        private final Paint p = P5.paint(0);
        private final Handler handler = new Handler(Looper.getMainLooper());
        private Bitmap weekday;
        private String weekdayKey = "";
        private final Runnable tick = new Runnable() {
            @Override
            public void run() {
                invalidate();
                schedule();
            }
        };

        CalendarCorner(Context c) {
            super(c);
            d = c.getResources().getDisplayMetrics().density;
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        private void schedule() {
            long now = System.currentTimeMillis();
            handler.postDelayed(tick, 60_000 - now % 60_000 + 50);
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            schedule();
        }

        @Override
        protected void onDetachedFromWindow() {
            handler.removeCallbacks(tick);
            super.onDetachedFromWindow();
        }

        @Override
        protected void onMeasure(int wSpec, int hSpec) {
            setMeasuredDimension((int) (470 * d), (int) (232 * d));
        }

        private void text(Canvas c, String s, float x, float y, float size, android.graphics.Typeface tf, int color,
                          float shadow, float spacing) {
            p.setTypeface(tf);
            p.setTextSize(size);
            p.setLetterSpacing(spacing);
            if (shadow > 0) {
                p.setColor(0xFF000000);
                c.drawText(s, x + shadow, y + shadow, p);
            }
            p.setColor(color);
            c.drawText(s, x, y, p);
            p.setLetterSpacing(0);
        }

        @Override
        protected void onDraw(Canvas c) {
            Calendar now = Calendar.getInstance();
            int hour = now.get(Calendar.HOUR_OF_DAY), min = now.get(Calendar.MINUTE);
            String wd = new SimpleDateFormat("EEE", Locale.ENGLISH).format(now.getTime()).toUpperCase(Locale.ENGLISH);
            if (weekday == null || !wd.equals(weekdayKey)) {
                weekday = P5.ransom(wd, 31 * d, 7);
                weekdayKey = wd;
            }
            boolean is24 = DateFormat.is24HourFormat(getContext());
            int h12 = hour % 12 == 0 ? 12 : hour % 12;
            String time = (is24 ? String.format(Locale.ENGLISH, "%02d", hour) : String.valueOf(h12))
                    + ":" + String.format(Locale.ENGLISH, "%02d", min);

            c.save();
            c.scale(d, d);
            c.translate(0, 30);
            c.rotate(-3f, 0, 70);
            // Date block: hard shadow, white edge, red body.
            p.setColor(0xFF000000);
            c.drawPath(P5.poly(26, 22, 312, 12, 298, 130, 10, 142), p);
            p.setColor(P5.WHITE);
            c.drawPath(P5.poly(14, 10, 304, 0, 290, 122, 0, 134), p);
            p.setColor(P5.RED);
            c.drawPath(P5.poly(19, 15, 298, 6, 285, 117, 6, 129), p);
            text(c, String.valueOf(now.get(Calendar.MONTH) + 1), 24, 100, 62, P5.anton, P5.WHITE, 3, 0);
            text(c, "/", 84, 114, 96, P5.anton, P5.BLACK, 0, 0);
            text(c, String.valueOf(now.get(Calendar.DAY_OF_MONTH)), 112, 112, 104, P5.anton, P5.WHITE, 5, 0);
            // Weekday as a ransom note, overlapping the block's right edge.
            c.save();
            c.scale(1f / d, 1f / d);
            c.translate(284 * d, 0 * d);
            c.rotate(-5f);
            c.drawBitmap(weekday, 0, 0, p);
            c.restore();
            // Time strip.
            p.setColor(0xFF000000);
            c.drawPath(P5.poly(14, 142, 392, 128, 384, 182, 0, 196), p);
            p.setColor(P5.RED);
            c.drawPath(P5.poly(0, 140, 14, 140, 4, 194, -4, 194), p);
            c.drawPath(P5.star(30, 164, 9, 0), p);
            text(c, timeOfDay(hour), 46, 174, 24, P5.bebas, P5.WHITE, 0, 0.12f);
            float tw;
            p.setTypeface(P5.anton);
            p.setTextSize(38);
            tw = p.measureText(time);
            text(c, time, 300 - tw + 40, 175, 38, P5.anton, P5.WHITE, 0, 0);
            if (!is24) text(c, hour < 12 ? "AM" : "PM", 346, 175, 20, P5.bebas, P5.RED, 0, 0.05f);
            c.restore();
        }
    }

    // ---- ransom title --------------------------------------------------------------------------

    /** The focused app's name as a ransom note. */
    static final class Ransom extends View {
        private Bitmap bmp;
        private String text = "";
        private final Paint p = new Paint(Paint.FILTER_BITMAP_FLAG);

        Ransom(Context c) {
            super(c);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        String text() {
            return text;
        }

        int contentHeight() {
            return bmp == null ? 0 : bmp.getHeight();
        }

        void setText(String s, float basePx) {
            text = s;
            bmp = P5.ransom(s, basePx, 11);
            requestLayout();
            invalidate();
        }

        @Override
        protected void onMeasure(int w, int h) {
            setMeasuredDimension(bmp == null ? 0 : bmp.getWidth(), bmp == null ? 0 : bmp.getHeight());
        }

        @Override
        protected void onDraw(Canvas c) {
            if (bmp != null) c.drawBitmap(bmp, 0, 0, p);
        }
    }

    // ---- wipe ----------------------------------------------------------------------------------

    /** Diagonal red / white / black wipe that sweeps off the screen, like Persona 5's transitions. */
    static final class Wipe extends View {
        private final Paint paint = P5.paint(0);
        private final Path path = new Path();
        private float t = 0f;
        private ValueAnimator anim;
        private static final int[] COLORS = {P5.BLACK, P5.WHITE, P5.RED};   // bottom to top
        private static final float[] DELAY = {0.24f, 0.12f, 0f};

        Wipe(Context c) {
            super(c);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        void play() {
            if (anim != null) anim.cancel();
            t = 0f;
            setVisibility(VISIBLE);
            anim = ValueAnimator.ofFloat(0f, 1f);
            anim.setDuration(520);
            anim.setInterpolator(new AccelerateDecelerateInterpolator());
            anim.addUpdateListener(a -> {
                t = (float) a.getAnimatedValue();
                invalidate();
            });
            anim.addListener(new android.animation.AnimatorListenerAdapter() {
                private boolean cancelled;

                @Override
                public void onAnimationCancel(android.animation.Animator animation) {
                    cancelled = true;
                }

                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    if (!cancelled) setVisibility(GONE);
                }
            });
            anim.start();
        }

        @Override
        protected void onDraw(Canvas c) {
            int w = getWidth(), h = getHeight();
            float skew = 0.28f * h;
            for (int i = 0; i < COLORS.length; i++) {
                float f = Math.max(0f, Math.min(1f, (t - DELAY[i]) / 0.76f));
                float e = -skew + f * (w + 2 * skew);
                path.reset();
                path.moveTo(e + skew, 0);
                path.lineTo(w + 2 * skew, 0);
                path.lineTo(w + 2 * skew, h);
                path.lineTo(e - skew, h);
                path.close();
                paint.setColor(COLORS[i]);
                c.drawPath(path, paint);
            }
        }
    }

    // ---- calling card (screensaver) --------------------------------------------------------------

    /** A cream calling card with a red band, a star, a ransom headline and the time. Drawn once a minute. */
    static final class CallingCard extends View {
        static final int W_DP = 800, H_DP = 450;
        private final float d;
        private final Paint p = P5.paint(0);
        private Bitmap headline;

        CallingCard(Context c) {
            super(c);
            d = c.getResources().getDisplayMetrics().density;
            setLayerType(LAYER_TYPE_HARDWARE, null);
        }

        @Override
        protected void onMeasure(int w, int h) {
            setMeasuredDimension((int) (W_DP * d), (int) (H_DP * d));
        }

        void refresh() {
            invalidate();
        }

        @Override
        protected void onDraw(Canvas c) {
            if (headline == null) headline = P5.ransom("TIME TO RELAX", 34 * d, 5);
            Calendar now = Calendar.getInstance();
            int hour = now.get(Calendar.HOUR_OF_DAY), min = now.get(Calendar.MINUTE);
            boolean is24 = DateFormat.is24HourFormat(getContext());
            int h12 = hour % 12 == 0 ? 12 : hour % 12;
            String time = (is24 ? String.format(Locale.ENGLISH, "%02d", hour) : String.valueOf(h12))
                    + ":" + String.format(Locale.ENGLISH, "%02d", min);
            String date = new SimpleDateFormat("EEEE  d MMMM", Locale.ENGLISH).format(now.getTime()).toUpperCase(Locale.ENGLISH);

            c.save();
            c.scale(d, d);
            // Card: shadow, then paper with slightly torn corners.
            p.setColor(0xFF000000);
            c.drawPath(P5.poly(24, 22, 790, 14, 786, 440, 14, 442), p);
            p.setColor(P5.CREAM);
            Path card = P5.poly(8, 6, 330, 0, 780, 8, 774, 430, 392, 438, 0, 432);
            c.drawPath(card, p);
            c.save();
            c.clipPath(card);
            // Red band with halftone and a star.
            p.setColor(P5.RED);
            c.drawPath(P5.poly(0, 0, 262, 0, 216, 450, 0, 450), p);
            p.setColor(0x66000000);
            P5.halftone(c, p, 0, 250, 230, 450, 15, 5.5f, true);
            p.setColor(P5.BLACK);
            c.drawPath(P5.star(120 + 7, 168 + 7, 92, 9), p);
            p.setColor(P5.WHITE);
            c.drawPath(P5.star(120, 168, 92, 9), p);
            p.setColor(P5.BLACK);
            c.drawPath(P5.star(120, 168, 40, 9), p);
            p.setColor(P5.BLACK);
            c.drawPath(P5.poly(262, 0, 276, 0, 230, 450, 216, 450), p);
            c.restore();
            // Headline.
            c.save();
            c.scale(1f / d, 1f / d);
            c.translate(300 * d, 22 * d);
            c.rotate(-2.5f);
            c.drawBitmap(headline, 0, 0, p);
            c.restore();
            // Time.
            p.setTypeface(P5.anton);
            p.setTextSize(196);
            float tw = p.measureText(time);
            p.setColor(P5.RED);
            c.drawText(time, 300 + 7, 345 + 7, p);
            p.setColor(P5.BLACK);
            c.drawText(time, 300, 345, p);
            if (!is24) {
                p.setTypeface(P5.bebas);
                p.setTextSize(46);
                p.setColor(P5.RED);
                c.drawText(hour < 12 ? "AM" : "PM", 300 + tw + 14, 345, p);
            }
            // Date strip.
            p.setColor(P5.BLACK);
            c.drawPath(P5.poly(290, 368, 766, 360, 760, 414, 284, 420), p);
            p.setTypeface(P5.bebas);
            p.setTextSize(34);
            p.setLetterSpacing(0.12f);
            p.setColor(P5.WHITE);
            c.drawText(date, 312, 404, p);
            p.setLetterSpacing(0);
            c.restore();
        }
    }

    /** Screensaver background: black with a red slash and halftone, kept dim so the card carries the screen. */
    static final class DreamBackdrop extends View {
        private final float d;
        private final Paint p = P5.paint(0);
        private Path slash, slash2;

        DreamBackdrop(Context c) {
            super(c);
            d = c.getResources().getDisplayMetrics().density;
        }

        @Override
        protected void onSizeChanged(int w, int h, int ow, int oh) {
            slash = P5.poly(0, 0.62f * h, 0.55f * w, 0, 0.72f * w, 0, 0, 0.88f * h);
            slash2 = P5.poly(w, 0.30f * h, w, h, 0.52f * w, h);
        }

        @Override
        protected void onDraw(Canvas c) {
            c.drawColor(0xFF050505);
            p.setColor(0xFF4A0008);
            c.drawPath(slash, p);
            p.setColor(0xFF1A1A1A);
            c.drawPath(slash2, p);
            p.setColor(0xFF7A000C);
            P5.halftone(c, p, 0, getHeight() * 0.55f, getWidth(), getHeight(), 18 * d, 5f * d, true);
        }
    }
}
