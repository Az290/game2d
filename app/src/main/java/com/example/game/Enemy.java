package com.example.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import java.util.Random;

public class Enemy {
    private float x, y;
    private float speedX, speedY;
    private int screenWidth, screenHeight;
    private Random random = new Random();
    private Sprite sprite;

    private int maxHp;
    private int hp;
    private int xpValue;
    private int type;

    public Enemy(Context context, int screenWidth, int screenHeight, int type) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.type = type;

        // Load sprite theo loại Enemy, với scaleFactor để phù hợp màn hình nhỏ
        switch (type) {
            case 0: // Enemy 1 - tóc vàng (6x6 sprite sheet)
                sprite = new Sprite(context, R.drawable.enemy, 6, 6, 0.7f); // nhỏ còn 70%
                maxHp = 100;
                xpValue = 20;
                break;

            case 1: // Enemy 2 - máy bay (ảnh tĩnh)
                sprite = new Sprite(context, R.drawable.enemy2, 1, 1, 0.4f); // nhỏ 40%
                maxHp = 150;
                xpValue = 30;
                break;

            case 2: // Enemy 3 - bọ lăn (1x6 sprite sheet)
                sprite = new Sprite(context, R.drawable.enemy3, 1, 6, 0.5f); // nhỏ 50%
                maxHp = 80;
                xpValue = 25;
                break;
        }

        reset();
    }

    public void reset() {
        x = random.nextInt(screenWidth - sprite.getFrameWidth());
        y = -sprite.getFrameHeight();

        switch (type) {
            case 0: // tóc vàng bay thẳng
                speedX = 0;
                speedY = 2 + random.nextFloat() * 1.5f;
                break;

            case 1: // máy bay bay chéo chậm
                speedX = (random.nextBoolean() ? -3 : 3);
                speedY = 2.5f;
                break;

            case 2: // bọ lăn nhanh
                speedX = (random.nextBoolean() ? -2 : 2);
                speedY = 3.5f;
                break;
        }

        hp = maxHp;
    }

    public void takeDamage(int dmg) {
        hp -= dmg;
    }

    public boolean isDead() { return hp <= 0; }

    public void update() {
        x += speedX;
        y += speedY;
        sprite.update();

        if (y > screenHeight || x < -sprite.getFrameWidth() || x > screenWidth + sprite.getFrameWidth()) {
            reset();
        }
    }

    public void draw(Canvas canvas, Paint paint) {
        sprite.draw(canvas, (int)x, (int)y, paint);

        // Thanh máu
        paint.setColor(android.graphics.Color.RED);
        float barWidth = sprite.getFrameWidth();
        canvas.drawRect(x, y - 10, x + barWidth, y - 5, paint);

        paint.setColor(android.graphics.Color.GREEN);
        float hpWidth = barWidth * ((float)hp / maxHp);
        canvas.drawRect(x, y - 10, x + hpWidth, y - 5, paint);
    }

    public Rect getRect() {
        return new Rect((int)x, (int)y,
                (int)(x + sprite.getFrameWidth()),
                (int)(y + sprite.getFrameHeight()));
    }

    public int getXPValue() { return xpValue; }
    public float getX() { return x; }
    public float getY() { return y; }

    // Hỗ trợ cho Star (nhân bản Enemy)
    public int getType() { return type; }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }
}