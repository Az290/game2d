package com.example.game;

import android.content.Context;
import android.graphics.*;
import java.util.Random;

public class Star {
    private Bitmap starBitmap;
    private float x, y;
    private int screenWidth, screenHeight;
    private Random random = new Random();
    private boolean active = true;
    private int size; // kích thước star

    public Star(Context context, int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        // Load ảnh gốc
        Bitmap original = BitmapFactory.decodeResource(context.getResources(), R.drawable.star);

        // Scale nhỏ lại (ví dụ 64x64 pixel)
        size = (int)(64 * context.getResources().getDisplayMetrics().density / 3f);
        // tùy chỉnh theo kích thước mong muốn

        starBitmap = Bitmap.createScaledBitmap(original, size, size, true);

        respawn();
    }

    public void respawn() {
        x = random.nextInt(screenWidth - starBitmap.getWidth());
        y = random.nextInt(screenHeight / 2); // spawn ở nửa trên màn hình
        active = true;
    }

    public void draw(Canvas canvas, Paint paint) {
        if (active) {
            canvas.drawBitmap(starBitmap, x, y, paint);
        }
    }

    public Rect getRect() {
        return new Rect((int)x, (int)y, (int)x+starBitmap.getWidth(), (int)y+starBitmap.getHeight());
    }

    public boolean isActive() { return active; }

    public void deactivate() { active = false; }
}