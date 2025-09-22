package com.example.game;

import android.graphics.*;

public class Projectile {
    float x, y;
    private float speedY = -15;
    private boolean active = true;
    private int width = 12, height = 24;
    private int damage = 25; // sát thương mỗi viên

    public Projectile(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void update() {
        y += speedY;
        if (y < -100) active = false;
    }

    public void draw(Canvas canvas, Paint paint) {
        if (!active) return;
        paint.setColor(Color.YELLOW);
        canvas.drawRect(getRect(), paint);
    }

    public Rect getRect() {
        return new Rect((int)x, (int)y, (int)x+width, (int)y+height);
    }

    public boolean isActive() { return active; }
    public void deactivate() { active = false; }
    public int getDamage(){ return damage; }
}