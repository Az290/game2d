package com.example.game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;

public class Sprite {
    private Bitmap spriteSheet;
    private int rows, cols;
    private int frameWidth, frameHeight;
    private int currentFrame = 0;
    private int currentRow = 0;
    private long lastFrameChangeTime = 0;
    private int frameLengthInMillisecond = 150;

    private Bitmap[] frameCache;

    // ✅ Constructor cũ để các file khác không bị lỗi
    public Sprite(Context context, int resId, int rows, int cols) {
        this(context, resId, rows, cols, 1.0f); // mặc định scaleFactor = 1.0 (kích thước gốc)
    }

    // ✅ Constructor mới có scaleFactor
    public Sprite(Context context, int resId, int rows, int cols, float scaleFactor) {
        // Load ảnh gốc
        Bitmap original = BitmapFactory.decodeResource(context.getResources(), resId);

        // Resize sprite sheet
        int newW = (int)(original.getWidth() * scaleFactor);
        int newH = (int)(original.getHeight() * scaleFactor);
        spriteSheet = Bitmap.createScaledBitmap(original, newW, newH, true);

        this.rows = rows;
        this.cols = cols;
        frameWidth = spriteSheet.getWidth() / cols;
        frameHeight = spriteSheet.getHeight() / rows;

        // Cắt frame và cache
        initializeCache();
    }

    private void initializeCache() {
        frameCache = new Bitmap[rows * cols];
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int index = row * cols + col;
                frameCache[index] = Bitmap.createBitmap(spriteSheet,
                        col * frameWidth,
                        row * frameHeight,
                        frameWidth,
                        frameHeight);
            }
        }
    }

    public void update() {
        long now = System.currentTimeMillis();
        if (now - lastFrameChangeTime > frameLengthInMillisecond) {
            currentFrame = (currentFrame + 1) % cols;
            lastFrameChangeTime = now;
        }
    }

    public void draw(Canvas canvas, int x, int y, Paint paint) {
        int index = currentRow * cols + currentFrame;
        if (index >= 0 && index < frameCache.length) {
            canvas.drawBitmap(frameCache[index], x, y, paint);
        }
    }

    public int getFrameWidth() { return frameWidth; }
    public int getFrameHeight() { return frameHeight; }

    public void setRow(int row) { currentRow = row; }

    // ✅ Thêm lại resetAnimation
    public void resetAnimation() {
        currentFrame = 0;
        lastFrameChangeTime = System.currentTimeMillis();
    }

    // ✅ Thêm dispose để giải phóng bộ nhớ
    public void dispose() {
        if (spriteSheet != null && !spriteSheet.isRecycled()) {
            spriteSheet.recycle();
            spriteSheet = null;
        }
        if (frameCache != null) {
            for (Bitmap frame : frameCache) {
                if (frame != null && !frame.isRecycled()) {
                    frame.recycle();
                }
            }
            frameCache = null;
        }
    }
}