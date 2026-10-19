package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/**
 * A translucent glass slab: soft sheen fill, bright top specular, crisp rim and a faint outer glow.
 * One instance floats over the grid and is moved with spring animations between tiles.
 */
public class GlassView extends View {

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint specular = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path clip = new Path();
    private final float density;
    private float radius;

    public GlassView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        radius = 20f * density;
        rim.setStyle(Paint.Style.STROKE);
        rim.setStrokeWidth(1.5f * density);
        rim.setColor(0xCCFFFFFF);
        glow.setStyle(Paint.Style.STROKE);
    }

    private boolean p5;
    private boolean jagged = true;
    private final Paint p5Paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /** Persona 5 mode: draws the jagged red shard instead of the glass slab. */
    /** Dock: torn, jagged shard. Grid: a clean slanted one. */
    void setJagged(boolean j) {
        if (j != jagged) {
            jagged = j;
            invalidate();
        }
    }

    void setP5(boolean on) {
        p5 = on;
        invalidate();
    }

    void setRadius(float px) {
        radius = px;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        fill.setShader(new LinearGradient(0, 0, 0, h, 0x3DFFFFFF, 0x12FFFFFF, Shader.TileMode.CLAMP));
        specular.setShader(new LinearGradient(0, 0, 0, h * 0.45f, 0x66FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (p5) {
            drawShard(canvas);
            return;
        }
        float inset = 6f * density;   // room for the glow
        rect.set(inset, inset, getWidth() - inset, getHeight() - inset);

        // Outer glow: three widening, fading strokes.
        for (int i = 1; i <= 3; i++) {
            glow.setStrokeWidth(i * 2f * density);
            glow.setColor((0x30 / i) << 24 | 0xFFFFFF);
            float o = i * density;
            canvas.drawRoundRect(rect.left - o, rect.top - o, rect.right + o, rect.bottom + o, radius + o, radius + o, glow);
        }

        canvas.drawRoundRect(rect, radius, radius, fill);

        clip.reset();
        clip.addRoundRect(rect, radius, radius, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(clip);
        canvas.drawRect(rect.left, rect.top, rect.right, rect.top + rect.height() * 0.45f, specular);
        canvas.restore();

        canvas.drawRoundRect(rect, radius, radius, rim);
    }

    /** Black hard shadow, white edge, red body: the selection shard of the Persona 5 menus. */
    private void drawShard(Canvas canvas) {
        float m = 8f * density;   // room for the shadow
        float l = m, t = m, r = getWidth() - m, b = getHeight() - m;
        if (!jagged) {
            float s = 14f * density, e = 3f * density;
            p5Paint.setColor(0xFF000000);
            canvas.drawPath(P5.poly(l + s + m, t + m, r + m, t + m, r - s + m, b + m, l + m, b + m), p5Paint);
            p5Paint.setColor(0xFFFFFFFF);
            canvas.drawPath(P5.poly(l + s, t, r, t, r - s, b, l, b), p5Paint);
            p5Paint.setColor(P5.RED);
            canvas.drawPath(P5.poly(l + s + e * 1.4f, t + e, r - e, t + e, r - s - e * 1.4f, b - e, l + e, b - e), p5Paint);
            return;
        }
        p5Paint.setColor(0xFF000000);
        canvas.drawPath(P5.shard(l + m, t + m, r + m, b + m, 1.0f), p5Paint);
        p5Paint.setColor(0xFFFFFFFF);
        canvas.drawPath(P5.shard(l, t, r, b, 1.0f), p5Paint);
        p5Paint.setColor(P5.RED);
        canvas.drawPath(P5.shard(l, t, r, b, 0.945f), p5Paint);
    }
}
