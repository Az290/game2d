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

    // boss/shooter bullets
    private List<EnemyBullet> bullets = new ArrayList<>();
    private long lastShot = 0;
    private int cooldown = 1500; // ms

    // zigzag, boss movement angle
    private float angle=0;

    public Enemy(Context context, int screenWidth, int screenHeight, int type) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.type = type;

        // Load sprite theo loại Enemy
        switch (type) {
            case 0: // Enemy 1 - tóc vàng
                sprite = new Sprite(context, R.drawable.enemy, 6, 6, 0.7f);
                maxHp = 100;
                xpValue = 20;
                break;

            case 1: // Enemy 2 - máy bay
                sprite = new Sprite(context, R.drawable.enemy2, 1, 1, 0.4f);
                maxHp = 150;
                xpValue = 30;
                break;

            case 2: // Enemy 3 - bọ lăn
                sprite = new Sprite(context, R.drawable.enemy3, 1, 6, 0.5f);
                maxHp = 80;
                xpValue = 25;
                cooldown = 1200;
                break;

            case 99: // BOSS
                // Boss scale gấp đôi enemy thường (~0.8f)
                sprite = new Sprite(context, R.drawable.enemy_boss, 1, 1, 0.8f);
                maxHp = 2000;
                xpValue = 200;
                cooldown = 700;
                break;
        }

        reset();
    }

    public void reset() {
        x = random.nextInt(Math.max(1, screenWidth - sprite.getFrameWidth()));
        y = -sprite.getFrameHeight();

        switch (type) {
            case 0: // tóc vàng bay thẳng
                speedX = 0; speedY = 2 + random.nextFloat()*1.5f;
                break;

            case 1: // máy bay bay chéo
                speedX = (random.nextBoolean()? -3: 3);
                speedY = 2.5f;
                break;

            case 2: // bọ lăn nhanh
                speedX = (random.nextBoolean()? -2: 2);
                speedY = 3.5f;
                break;

            case 99: // Boss cố định trên đầu màn hình
                x = screenWidth/2f - sprite.getFrameWidth()/2f;
                y = 80;
                speedX=0; speedY=0;
                break;
        }

        hp = maxHp;
    }

    public void takeDamage(int dmg) {
        hp -= dmg;
    }

    public boolean isDead() { return hp <= 0; }

    public void update() {
        switch(type){
            case 0:
            case 1:
            case 2:
                x += speedX;
                y += speedY;
                break;

            case 99: // Boss di chuyển qua lại
                angle += 0.05f;
                x = (float)(screenWidth/2 + Math.sin(angle)*200);

                // Boss bắn nhiều viên đạn
                long now2 = System.currentTimeMillis();
                if(now2-lastShot>cooldown){
                    for(int i=-2;i<=2;i++){
                        bullets.add(new EnemyBullet(
                                x+sprite.getFrameWidth()/2,
                                y+sprite.getFrameHeight(),
                                i*2, 7));
                    }
                    lastShot=now2;
                }
                break;
        }

        sprite.update();

        // update bullet
        for(int i=bullets.size()-1;i>=0;i--){
            EnemyBullet b=bullets.get(i);
            b.update();
            if(!b.isActive()) bullets.remove(i);
        }

        // reset enemy thường khi ra khỏi màn hình
        if(type!=99 &&
                (y > screenHeight || x < -sprite.getFrameWidth() || x > screenWidth + sprite.getFrameWidth()))
            reset();
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

        // Vẽ bullet (nếu có)
        for(EnemyBullet b: bullets) b.draw(canvas,paint);
    }

    public Rect getRect() {
        return new Rect((int)x, (int)y,
                (int)(x + sprite.getFrameWidth()),
                (int)(y + sprite.getFrameHeight()));
    }

    public int getXPValue() { return xpValue; }
    public float getX() { return x; }
    public float getY() { return y; }
    public int getType() { return type; }
    public List<EnemyBullet> getBullets(){ return bullets; }

    public void setPosition(float x, float y) {
        this.x = x; this.y = y;
    }
}

/** Bullet class cho Shooter & Boss */
class EnemyBullet {
    float x,y,vx,vy;
    int size=10;
    boolean active=true;

    EnemyBullet(float x,float y,float vx,float vy){
        this.x=x; this.y=y; this.vx=vx; this.vy=vy;
    }

    public void update(){
        x+=vx; y+=vy;
        if(y>2000) active=false;
    }

    public void draw(Canvas c,Paint p){
        if(!active) return;
        p.setColor(Color.RED);
        c.drawCircle(x,y,size,p);
    }

    public Rect getRect(){
        return new Rect((int)(x-size),(int)(y-size),(int)(x+size),(int)(y+size));
    }
    public boolean isActive(){ return active; }
}