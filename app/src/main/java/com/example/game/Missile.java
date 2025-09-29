package com.example.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

public class Missile {
    private float x, y;
    private float vx, vy;
    private float speed = 8f;
    private boolean active = true;
    private int damage = 50;
    private int size = 20; // hiển thị lớn hơn bullet thường

    public Missile(float x, float y) {
        this.x = x;
        this.y = y;
    }

    // Update: hướng về Enemy gần nhất
    public void update(Enemy target) {
        if (!active || target == null) return;

        float dx = target.getX() - x;
        float dy = target.getY() - y;
        float dist = (float)Math.sqrt(dx*dx + dy*dy);
        if (dist > 1) {
            vx = (dx / dist) * speed;
            vy = (dy / dist) * speed;
        }

        x += vx;
        y += vy;

        // nếu bay ra khỏi màn hình -> deactivate
        if (y < -100 || x < -100 || x > 2000) {
            active = false;
        }
    }

    public void draw(Canvas canvas, Paint paint) {
        if (!active) return;
        paint.setColor(Color.CYAN);
        canvas.drawCircle(x, y, size/2, paint);
    }

    public Rect getRect() {
        return new Rect((int)(x - size/2), (int)(y - size/2),
                (int)(x + size/2), (int)(y + size/2));
    }

    public boolean isActive() { return active; }
    public void deactivate() { active = false; }
    public int getDamage() { return damage; }
}