package com.example.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

public class Item {
    public static final int TYPE_MISSILE = 0;
    public static final int TYPE_LASER   = 1;
    public static final int TYPE_SHIELD  = 2;
    public static final int TYPE_ARMOR   = 3;
    public static final int TYPE_BARRIER = 4;

    private int type;
    private float x, y;
    private float speedY = 4;
    private boolean active = true;
    private int size = 40; // kích thước hiển thị item

    public Item(int type, float x, float y) {
        this.type = type;
        this.x = x;
        this.y = y;
    }

    public void update() {
        y += speedY;
        if (y > 2000) active = false; // tạm thời hardcode
    }

    public void draw(Canvas canvas, Paint paint) {
        if (!active) return;

        switch (type) {
            case TYPE_MISSILE:
                paint.setColor(Color.CYAN);
                canvas.drawCircle(x, y, size/2f, paint);
                break;
            case TYPE_LASER:
                paint.setColor(Color.RED);
                canvas.drawRect(x - size/4f, y - size/2f, x + size/4f, y + size/2f, paint);
                break;
            case TYPE_SHIELD:
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(4);
                paint.setColor(Color.CYAN);
                canvas.drawCircle(x, y, size/2f, paint);
                paint.setStyle(Paint.Style.FILL);
                break;
            case TYPE_ARMOR:
                paint.setColor(Color.MAGENTA);
                canvas.drawCircle(x, y, size/2f, paint);
                break;
            case TYPE_BARRIER:
                paint.setColor(Color.YELLOW);
                canvas.drawRect(x - size/2f, y - size/4f, x + size/2f, y + size/4f, paint);
                break;
        }
    }

    public Rect getRect() {
        return new Rect((int)(x - size/2f), (int)(y - size/2f),
                (int)(x + size/2f), (int)(y + size/2f));
    }

    public boolean isActive() { return active; }
    public void deactivate() { active = false; }
    public int getType() { return type; }
}