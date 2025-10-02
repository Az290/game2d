package com.example.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import java.util.ArrayList;
import java.util.List;
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

    // Shooter/Boss bullets
    private List<EnemyBullet> bullets = new ArrayList<>();
    private long lastShot = 0;
    private int cooldown = 1500; // ms

    // Zigzag/boss movement
    private float angle = 0;
    private boolean escaped = false;
    public boolean hasEscaped() { return escaped; }

    public Enemy(Context context, int screenWidth, int screenHeight, int type) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.type = type;

        // Load sprite theo loại
        switch (type) {
            case 0: // Straight
                sprite = new Sprite(context, R.drawable.enemy, 1, 1, 0.35f);
                maxHp = 100; xpValue = 20;
                break;
            case 1: // Zigzag
                sprite = new Sprite(context, R.drawable.enemy2, 1, 1, 0.4f);
                maxHp = 120; xpValue = 30;
                break;
            case 2: // Shooter
                sprite = new Sprite(context, R.drawable.enemy3, 1, 1, 0.1f);
                maxHp = 150; xpValue = 40;
                cooldown = 1200;
                break;
            case 99: // Boss
                sprite = new Sprite(context, R.drawable.enemy_boss, 1, 1, 0.8f);
                maxHp = 2000; xpValue = 200;
                cooldown = 700;
                break;
        }

        // spawn trên top
        x = random.nextInt(Math.max(1, screenWidth - sprite.getFrameWidth()));
        y = -sprite.getFrameHeight();
        hp = maxHp;

        switch (type) {
            case 0:
                speedX=0; speedY=3; break;
            case 1:
                speedX=0; speedY=2; break;
            case 2:
                speedX=0; speedY=2; break;
            case 99:
                x = screenWidth/2f - sprite.getFrameWidth()/2f;
                y = 80;
                speedX=0; speedY=0;
                break;
        }
    }

    public void takeDamage(int dmg) { hp -= dmg; }
    public boolean isDead() { return hp <= 0; }

    public void update() {
        long now = System.currentTimeMillis();

        switch (type) {
            case 0: // Straight mover
                y += speedY;
                // Attack: twin cyan bullets in V-shape
                if (y > 100 && now - lastShot > cooldown) {
                    bullets.add(new EnemyBullet(
                            x + sprite.getFrameWidth()/2,
                            y + sprite.getFrameHeight(),
                            -3, 6, Color.CYAN, 12));
                    bullets.add(new EnemyBullet(
                            x + sprite.getFrameWidth()/2,
                            y + sprite.getFrameHeight(),
                            3, 6, Color.CYAN, 12));
                    lastShot = now;
                }
                break;

            case 1: // Zigzag mover
                y += speedY;
                angle += 0.1f;
                x = (float)(screenWidth/2 + Math.sin(angle) * 150);

                // Attack: spread 3 magenta bullets
                if (y > 100 && now - lastShot > cooldown) {
                    for (int i = -1; i <= 1; i++) {
                        bullets.add(new EnemyBullet(
                                x + sprite.getFrameWidth()/2,
                                y + sprite.getFrameHeight(),
                                i * 4, 6, Color.MAGENTA, 14));
                    }
                    lastShot = now;
                }
                break;

            case 2: // Shooter
                y += speedY;
                if (y > 150) speedY = 0;

                // Attack: single red bullet straight down
                if (now - lastShot > cooldown) {
                    bullets.add(new EnemyBullet(
                            x + sprite.getFrameWidth()/2,
                            y + sprite.getFrameHeight(),
                            0, 6, Color.RED, 10));
                    lastShot = now;
                }
                break;

            case 99: // Boss
                angle += 0.05f;
                x = (float)(screenWidth/2 + Math.sin(angle) * 200);

                // Attack: 5 orange bullets in spread
                if (now - lastShot > cooldown) {
                    for (int i = -2; i <= 2; i++) {
                        bullets.add(new EnemyBullet(
                                x + sprite.getFrameWidth()/2,
                                y + sprite.getFrameHeight(),
                                i * 2, 7, Color.rgb(255,100,0), 16));
                    }
                    lastShot = now;
                }
                break;
        }

        // Clamp ngang để không văng ngoài màn hình
        if (x < 0) x = 0;
        if (x > screenWidth - sprite.getFrameWidth())
            x = screenWidth - sprite.getFrameWidth();

        // Kiểm tra enemy thoát màn hình dưới (trừ máu player)
        if ((type == 0 || type == 1) && y > screenHeight) {
            escaped = true;
            hp = 0; // để remove
        }

        // Update sprite
        sprite.update();

        // Update bullets
        for (int i = bullets.size() - 1; i >= 0; i--) {
            EnemyBullet b = bullets.get(i);
            b.update();
            if (!b.isActive()) bullets.remove(i);
        }
    }

    public void draw(Canvas c, Paint p) {
        sprite.draw(c,(int)x,(int)y,p);

        // HP bar
        p.setColor(Color.RED);
        float bw=sprite.getFrameWidth();
        c.drawRect(x,y-10,x+bw,y-5,p);
        p.setColor(Color.GREEN);
        float hw=bw*((float)hp/maxHp);
        c.drawRect(x,y-10,x+hw,y-5,p);

        for(EnemyBullet b: bullets) b.draw(c,p);
    }

    public Rect getRect(){
        return new Rect((int)x,(int)y,
                (int)(x+sprite.getFrameWidth()),
                (int)(y+sprite.getFrameHeight()));
    }

    public int getXPValue(){return xpValue;}
    public float getX(){return x;}
    public float getY(){return y;}
    public int getType(){return type;}
    public List<EnemyBullet> getBullets(){return bullets;}
    public void setPosition(float x,float y){this.x=x;this.y=y;}
}

/** Bullet Class */
class EnemyBullet {
    float x, y, vx, vy;
    int size;
    boolean active = true;
    int color;

    // Constructor mặc định (giữ tương thích cũ)
    EnemyBullet(float x, float y, float vx, float vy) {
        this(x, y, vx, vy, Color.RED, 10);
    }

    // Constructor mới với màu + size tuỳ biến
    EnemyBullet(float x, float y, float vx, float vy, int color, int size) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.color = color;
        this.size = size;
    }

    public void update() {
        x += vx;
        y += vy;
        if (y > 2200 || y < -100 || x < -200 || x > 2200) {
            active = false;
        }
    }

    public void draw(Canvas c, Paint p) {
        if (!active) return;
        p.setColor(color);
        c.drawCircle(x, y, size, p);
    }

    public Rect getRect() {
        return new Rect((int)(x - size), (int)(y - size),
                (int)(x + size), (int)(y + size));
    }

    public boolean isActive() { return active; }
}