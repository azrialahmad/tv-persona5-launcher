package com.gathin.tvhome;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.service.dreams.DreamService;
import android.view.View;

import java.util.Random;

/**
 * Screensaver: the launcher's thin clock and date on black, over a slow ambient glow.
 * Every minute the clock drifts to a new spot and the glow shifts colour, so nothing burns in.
 * No network, no images: cheap enough for a 1 GB TV.
 */
public class ClockDream extends DreamService {

    private static final long MOVE_MS = 60_000L;
    /** Deep, low-brightness glow colours; the glow is drawn at partial alpha on black. */
    private static final int[] PALETTE = {0xFF2B5CFF, 0xFF00A39A, 0xFF8A3FFC, 0xFFE0661B, 0xFFD6336C};

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private View content;
    private GradientDrawable glow;
    private int colorIndex;
    private int glowColor;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (card != null) {
                cardMove();
                handler.postDelayed(this, MOVE_MS);
                return;
            }
            moveClock();
            shiftGlow();
            handler.postDelayed(this, MOVE_MS);
        }
    };

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);   // any key wakes the TV back to where it was
        setFullscreen(true);
        setScreenBright(false);
        if (Config.load(this).persona5) {
            setupCallingCard();
            return;
        }
        setContentView(R.layout.dream_clock);

        content = findViewById(R.id.dreamContent);
        View ambient = findViewById(R.id.dreamAmbient);
        glow = new GradientDrawable();
        glow.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        ambient.setBackground(glow);
        ambient.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) ->
                glow.setGradientRadius((r - l) * 0.55f));
        colorIndex = random.nextInt(PALETTE.length);
        setGlow(PALETTE[colorIndex]);
    }

    // ---- Persona 5 calling card ---------------------------------------------------------------

    private P5Views.CallingCard card;
    private android.widget.FrameLayout stage;

    private void setupCallingCard() {
        P5.init(this);
        stage = new android.widget.FrameLayout(this);
        stage.addView(new P5Views.DreamBackdrop(this), new android.widget.FrameLayout.LayoutParams(-1, -1));
        card = new P5Views.CallingCard(this);
        card.setAlpha(0f);
        stage.addView(card, new android.widget.FrameLayout.LayoutParams(-2, -2));
        setContentView(stage);
    }

    /** Puts the card at a fresh spot and tilt (always fully on screen, away from the edges to spare the panel). */
    private void placeCard() {
        int w = stage.getWidth(), h = stage.getHeight();
        float x = Math.max(0, w - card.getMeasuredWidth()) * (0.15f + 0.7f * random.nextFloat());
        float y = Math.max(0, h - card.getMeasuredHeight()) * (0.15f + 0.7f * random.nextFloat());
        card.setPivotX(card.getMeasuredWidth() / 2f);
        card.setPivotY(card.getMeasuredHeight() / 2f);
        card.setX(x);
        card.setY(y);
        card.setRotation((random.nextFloat() - 0.5f) * 10f);
    }

    /** The card slams in from the left with an overshoot. */
    private void cardIn() {
        card.refresh();
        card.setTranslationX(-stage.getWidth() * 0.5f);
        card.setScaleX(1.25f);
        card.setScaleY(1.25f);
        card.setAlpha(0f);
        card.animate().translationX(0f).scaleX(1f).scaleY(1f).alpha(1f).setDuration(520)
                .setInterpolator(new android.view.animation.OvershootInterpolator(1.4f)).start();
    }

    private void cardMove() {
        card.animate().translationXBy(stage.getWidth() * 0.6f).rotationBy(14f).alpha(0f).setDuration(380)
                .setInterpolator(new android.view.animation.AccelerateInterpolator(1.6f))
                .withEndAction(() -> {
                    placeCard();
                    cardIn();
                }).start();
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        if (card != null) {
            stage.post(() -> {
                placeCard();
                cardIn();
            });
            handler.postDelayed(tick, MOVE_MS);
            return;
        }
        content.setAlpha(0f);
        content.post(() -> {
            placeClock();
            content.animate().alpha(1f).setDuration(1200).start();
        });
        handler.postDelayed(tick, MOVE_MS);
    }

    @Override
    public void onDreamingStopped() {
        handler.removeCallbacks(tick);
        super.onDreamingStopped();
    }

    @Override
    public void onDetachedFromWindow() {
        handler.removeCallbacks(tick);
        super.onDetachedFromWindow();
    }

    private void moveClock() {
        content.animate().alpha(0f).setDuration(900).withEndAction(() -> {
            placeClock();
            content.animate().alpha(1f).setDuration(1200).start();
        }).start();
    }

    /** A random spot that keeps the whole clock on screen, with the glow centred behind it. */
    private void placeClock() {
        View parent = (View) content.getParent();
        int maxX = Math.max(0, parent.getWidth() - content.getWidth());
        int maxY = Math.max(0, parent.getHeight() - content.getHeight());
        float x = maxX * (0.1f + 0.8f * random.nextFloat());
        float y = maxY * (0.1f + 0.8f * random.nextFloat());
        content.setX(x);
        content.setY(y);
        if (parent.getWidth() > 0 && parent.getHeight() > 0) {
            glow.setGradientCenter((x + content.getWidth() / 2f) / parent.getWidth(),
                    (y + content.getHeight() / 2f) / parent.getHeight());
        }
    }

    private void shiftGlow() {
        int from = glowColor;
        colorIndex = (colorIndex + 1) % PALETTE.length;
        ValueAnimator anim = ValueAnimator.ofObject(new ArgbEvaluator(), from, PALETTE[colorIndex]);
        anim.setDuration(8000);
        anim.addUpdateListener(a -> setGlow((int) a.getAnimatedValue()));
        anim.start();
    }

    private void setGlow(int color) {
        glowColor = color;
        int c = color & 0x00FFFFFF;
        glow.setColors(new int[]{0x40000000 | c, 0x14000000 | c, 0x00000000 | c});
    }
}
