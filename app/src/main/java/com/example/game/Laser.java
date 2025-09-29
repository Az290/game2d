package com.example.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

public class Laser {
    private float x, y;
    private int width = 18;      // độ rộng tia laser (hơi to một chút)
    private int damage = 100;
    private boolean active = true;
    private long startTime;
    private static final long DURATION = 300; // tồn tại 0.3s

    private int screenHeight;

    public Laser(float x, float y, int screenHeight) {
        this.x = x;
        this.y = y;
        this.screenHeight = screenHeight;
        this.startTime = System.currentTimeMillis();
    }

    public void update() {
        if (System.currentTimeMillis() - startTime > DURATION) {
            active = false;
        }
    }

    public void draw(Canvas canvas, Paint paint) {
        if (!active) return;
        // Laser màu đỏ rực
        paint.setColor(Color.RED);
        paint.setStrokeWidth(width);
        // Vẽ line từ player lên trên cùng màn hình
        canvas.drawLine(x, y, x, 0, paint);
    }

    // Check enemy có bị "xuyên" qua
    public boolean hit(Enemy enemy) {
        if (!active) return false;
        Rect rect = enemy.getRect();
        // Laser thẳng đứng, enemy bị trúng khi đứng chặn đúng cột x
        return (rect.left < x && rect.right > x && rect.top < y);
    }

    public boolean isActive() { return active; }
    public int getDamage() { return damage; }
}