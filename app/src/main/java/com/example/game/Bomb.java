package com.example.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class Bomb {
    private float x, y;
    private boolean exploded = false;
    private boolean active = true;

    private final float fallSpeed = 8f;     // tốc độ rơi
    private final int damage = 80;
    private final float explosionRadius = 120;

    private final long dropTime;                          // thời điểm thả
    private static final long FUSE_TIME = 1200;           // sau 1.2 giây thì nổ
    private static final long EXPLOSION_DURATION = 500;   // vòng nổ tồn tại 0.5 giây
    private long explosionStartTime;

    public Bomb(float x, float y) {
        this.x = x;
        this.y = y;
        dropTime = System.currentTimeMillis();
    }

    public void update() {
        if (!active) return;

        if (!exploded) {
            // Bom rơi cho đến khi phát nổ
            y += fallSpeed;

            if (System.currentTimeMillis() - dropTime > FUSE_TIME) {
                exploded = true;
                explosionStartTime = System.currentTimeMillis();
            }
        } else {
            // Sau khi nổ 0.5s thì bom biến mất
            if (System.currentTimeMillis() - explosionStartTime > EXPLOSION_DURATION) {
                active = false;
            }
        }
    }

    public void draw(Canvas canvas, Paint paint) {
        if (!active) return;

        if (!exploded) {
            // Quả bom lúc rơi (màu xám)
            paint.setColor(Color.DKGRAY);
            canvas.drawCircle(x, y, 20, paint);
        } else {
            // Vụ nổ (màu cam đỏ, bán kính lớn)
            paint.setColor(Color.argb(180, 255, 80, 0));
            canvas.drawCircle(x, y, explosionRadius, paint);
        }
    }

    // Check enemy có trong vùng nổ hay không
    public boolean intersects(Enemy enemy) {
        if (!exploded) return false;
        float dx = enemy.getX() - x;
        float dy = enemy.getY() - y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        return dist < explosionRadius;
    }

    public boolean isActive() { return active; }

    public int getDamage() { return damage; }
}