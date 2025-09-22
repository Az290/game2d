package com.example.game;

import android.content.Context;
import android.graphics.*;
import java.util.Random;

public class Enemy {
    private float x, y;
    private float speedY;
    private int screenWidth, screenHeight;
    private Random random = new Random();
    private Sprite sprite;

    private int maxHp = 100;
    private int hp;

    public Enemy(Context context, int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        sprite = new Sprite(context, R.drawable.enemy, 6, 6);
        reset();
    }

    public void reset() {
        x = random.nextInt(screenWidth - sprite.getFrameWidth());
        y = -sprite.getFrameHeight();
        speedY = 2 + random.nextFloat() * 1.5f;
        hp = maxHp;
    }

    public void takeDamage(int dmg) {
        hp -= dmg;
    }

    public boolean isDead() { return hp <= 0; }

    public void update() {
        y += speedY;
        sprite.update();
        if (y > screenHeight) reset();
    }

    public void draw(Canvas canvas, Paint paint) {
        sprite.draw(canvas, (int)x, (int)y, paint);

        // Thanh máu
        paint.setColor(Color.RED);
        float barWidth = sprite.getFrameWidth();
        canvas.drawRect(x, y - 10, x + barWidth, y - 5, paint);
        paint.setColor(Color.GREEN);
        float hpWidth = barWidth * ((float)hp / maxHp);
        canvas.drawRect(x, y - 10, x + hpWidth, y - 5, paint);
    }

    public Rect getRect() {
        if (y < 0) {
            return new Rect(0,0,0,0);
        }
        return new Rect((int)x,(int)y,(int)x+sprite.getFrameWidth(), (int)y+sprite.getFrameHeight());
    }

    public float getX() { return x; }
    public float getY() { return y; }
}