package com.example.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

public class Barrier {
    private float x, y;
    private int width=120;
    private int height=20;
    private boolean active = false;

    public void activate(float px, float py) {
        this.x = px;
        this.y = py - 60; // phía trước player
        active = true;
    }

    public void draw(Canvas canvas, Paint paint) {
        if (!active) return;
        paint.setColor(Color.YELLOW);
        canvas.drawRect(x-width/2, y, x+width/2, y+height, paint);
    }

    public boolean isActive() { return active; }
    public void deactivate() { active=false; }

    public Rect getRect() {
        return new Rect((int)(x-width/2),(int)y,(int)(x+width/2),(int)(y+height));
    }
}