package com.example.game;

import android.content.Context;
import android.graphics.*;

public class Explosion {
    private Bitmap sheet;
    private int frameWidth, frameHeight;
    private int totalFrames;
    private int currentFrame = 0;
    private long lastFrameTime;
    private int frameDuration = 80; // ms

    private float x, y;
    private boolean finished = false;

    public Explosion(Context context, int resId, float x, float y, int cols) {
        this.sheet = BitmapFactory.decodeResource(context.getResources(), resId);
        this.totalFrames = cols;
        this.frameWidth = sheet.getWidth() / cols;
        this.frameHeight = sheet.getHeight();
        this.x = x;
        this.y = y;
    }

    public void update() {
        long now = System.currentTimeMillis();
        if (now - lastFrameTime > frameDuration) {
            currentFrame++;
            lastFrameTime = now;
            if (currentFrame >= totalFrames) finished = true;
        }
    }

    public void draw(Canvas canvas, Paint paint) {
        if (finished) return;
        int srcX = currentFrame * frameWidth;
        Rect src = new Rect(srcX,0, srcX+frameWidth, frameHeight);
        Rect dst = new Rect((int)x, (int)y, (int)(x+frameWidth), (int)(y+frameHeight));
        canvas.drawBitmap(sheet, src, dst, paint);
    }

    public boolean isFinished() { return finished; }
}