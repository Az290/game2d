package com.example.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class Shield {
    private int hitsLeft = 3;
    private boolean active = false;
    private float x, y, radius;

    public void activate(float x, float y, float radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.hitsLeft = 3;
        this.active = true;
    }

    public void hit() {
        hitsLeft--;
        if (hitsLeft <= 0) active = false;
    }

    public void update(float px, float py) {
        this.x = px;
        this.y = py;
    }

    public void draw(Canvas canvas, Paint paint) {
        if (!active) return;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6);
        paint.setColor(Color.CYAN);
        canvas.drawCircle(x, y, radius, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    public boolean isActive() { return active; }
}