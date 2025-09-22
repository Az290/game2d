package com.example.game;

import android.content.Context;
import android.graphics.*;

public class Sprite {
    private Bitmap spriteSheet;
    private int rows, cols;
    private int frameWidth, frameHeight;
    private int currentFrame = 0;
    private int currentRow = 0;
    private long lastFrameChangeTime = 0;
    private int frameLengthInMillisecond = 150;
    private final int ROW_ATTACK = 2;  // row của motion chém kiếm trong sprite sheet

    // Thêm biến để kiểm soát trạng thái animation
    private boolean isMoving = false;
    private boolean needsReset = false;
    private int targetRow = 0;

    // Cache bitmap để tránh vẽ lại liên tục
    private Bitmap[] frameCache;
    private Matrix transformMatrix;

    public Sprite(Context context, int resId, int rows, int cols) {
        spriteSheet = BitmapFactory.decodeResource(context.getResources(), resId);
        this.rows = rows;
        this.cols = cols;

        if (spriteSheet != null && cols > 0 && rows > 0) {
            frameWidth = spriteSheet.getWidth() / cols;
            frameHeight = spriteSheet.getHeight() / rows;

            // Khởi tạo cache và matrix
            initializeCache();
            transformMatrix = new Matrix();
        } else {
            frameWidth = frameHeight = 0;
        }
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

    public void setRow(int row) {
        if (row >= 0 && row < rows) {
            if (currentRow != row) {
                targetRow = row;
                needsReset = true;
                isMoving = (row != 0);
            }
        }
    }

    public void resetAnimation() {
        if (needsReset) {
            currentFrame = 0;
            currentRow = targetRow;
            lastFrameChangeTime = System.currentTimeMillis();
            needsReset = false;
        }
    }

    public void update() {
        if (!isMoving && currentRow == 0) {
            currentFrame = 0;
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastFrameChangeTime > frameLengthInMillisecond) {
            currentFrame = (currentFrame + 1) % cols;
            lastFrameChangeTime = now;
        }
    }

    public void draw(Canvas canvas, int x, int y, Paint paint) {
        if (frameCache == null || frameWidth == 0 || frameHeight == 0) return;

        // Sử dụng frame từ cache
        int index = currentRow * cols + currentFrame;
        if (index >= 0 && index < frameCache.length) {
            transformMatrix.reset();
            transformMatrix.postTranslate(x, y);

            canvas.drawBitmap(frameCache[index], transformMatrix, paint);
        }
    }

    public int getFrameWidth() {
        return frameWidth;
    }

    public int getFrameHeight() {
        return frameHeight;
    }

    public int getCurrentRow() {
        return currentRow;
    }

    public void stopAnimation() {
        isMoving = false;
        currentFrame = 0;
        needsReset = true;
    }

    // Thêm phương thức để giải phóng bộ nhớ
    public void dispose() {
        if (spriteSheet != null) {
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