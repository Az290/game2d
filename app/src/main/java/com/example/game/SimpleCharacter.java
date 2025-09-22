package com.example.game;

import android.graphics.*;

public class SimpleCharacter {
    private float x, y;
    private int width = 64;
    private int height = 64;

    // Animation states
    private enum State {
        IDLE, WALKING_RIGHT, WALKING_LEFT, WALKING_UP, WALKING_DOWN
    }
    private State currentState = State.IDLE;

    // Animation
    private float animationTime = 0;
    private int currentFrame = 0;
    private int frameCount = 4;
    private float frameDelay = 0.15f; // seconds per frame
    private float frameTimer = 0;

    // Colors for character
    private Paint bodyPaint, headPaint, eyePaint, legPaint;

    public SimpleCharacter(float x, float y) {
        this.x = x;
        this.y = y;

        // Initialize paints
        bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bodyPaint.setColor(Color.rgb(100, 150, 200)); // Blue body

        headPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        headPaint.setColor(Color.rgb(255, 220, 177)); // Skin color

        eyePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        eyePaint.setColor(Color.BLACK);

        legPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        legPaint.setColor(Color.rgb(50, 50, 50)); // Dark gray
    }

    public void setState(float speedX, float speedY) {
        if (speedX == 0 && speedY == 0) {
            currentState = State.IDLE;
        } else if (Math.abs(speedX) > Math.abs(speedY)) {
            currentState = speedX > 0 ? State.WALKING_RIGHT : State.WALKING_LEFT;
        } else {
            currentState = speedY > 0 ? State.WALKING_DOWN : State.WALKING_UP;
        }
    }

    public void update(float deltaTime) {
        animationTime += deltaTime;
        frameTimer += deltaTime;

        // Update frame
        if (frameTimer >= frameDelay) {
            frameTimer = 0;
            currentFrame = (currentFrame + 1) % frameCount;
        }
    }

    public void draw(Canvas canvas, float x, float y) {
        this.x = x;
        this.y = y;

        canvas.save();
        canvas.translate(x, y);

        // Draw based on state
        switch (currentState) {
            case IDLE:
                drawIdle(canvas);
                break;
            case WALKING_RIGHT:
                drawWalking(canvas, false);
                break;
            case WALKING_LEFT:
                canvas.scale(-1, 1); // Flip horizontally
                drawWalking(canvas, false);
                break;
            case WALKING_UP:
            case WALKING_DOWN:
                drawWalking(canvas, true);
                break;
        }

        canvas.restore();
    }

    private void drawIdle(Canvas canvas) {
        // Breathing animation
        float breathScale = 1f + (float)Math.sin(animationTime * 3) * 0.02f;
        canvas.scale(1f, breathScale);

        // Draw shadow
        Paint shadowPaint = new Paint();
        shadowPaint.setColor(Color.BLACK);
        shadowPaint.setAlpha(50);
        canvas.drawOval(-20, 25, 20, 35, shadowPaint);

        // Draw legs
        canvas.drawRect(-8, 10, -3, 25, legPaint);
        canvas.drawRect(3, 10, 8, 25, legPaint);

        // Draw body
        canvas.drawRoundRect(-15, -10, 15, 15, 5, 5, bodyPaint);

        // Draw arms
        canvas.drawRect(-18, -5, -13, 10, bodyPaint);
        canvas.drawRect(13, -5, 18, 10, bodyPaint);

        // Draw head
        canvas.drawCircle(0, -20, 12, headPaint);

        // Draw eyes
        canvas.drawCircle(-4, -22, 2, eyePaint);
        canvas.drawCircle(4, -22, 2, eyePaint);

        // Draw mouth
        Paint mouthPaint = new Paint();
        mouthPaint.setColor(Color.BLACK);
        mouthPaint.setStyle(Paint.Style.STROKE);
        mouthPaint.setStrokeWidth(1.5f);
        Path mouth = new Path();
        mouth.moveTo(-3, -16);
        mouth.quadTo(0, -14, 3, -16);
        canvas.drawPath(mouth, mouthPaint);
    }

    private void drawWalking(Canvas canvas, boolean vertical) {
        // Walking animation
        float walkOffset = (float)Math.sin(currentFrame * Math.PI / 2) * 3;
        float armSwing = (float)Math.sin(currentFrame * Math.PI / 2) * 15;

        // Draw shadow
        Paint shadowPaint = new Paint();
        shadowPaint.setColor(Color.BLACK);
        shadowPaint.setAlpha(50);
        canvas.drawOval(-20, 25, 20, 35, shadowPaint);

        // Draw legs with walking animation
        canvas.save();
        canvas.translate(0, Math.abs(walkOffset));

        // Left leg
        canvas.save();
        canvas.rotate(armSwing, -5, 10);
        canvas.drawRect(-8, 10, -3, 25, legPaint);
        canvas.restore();

        // Right leg
        canvas.save();
        canvas.rotate(-armSwing, 5, 10);
        canvas.drawRect(3, 10, 8, 25, legPaint);
        canvas.restore();

        // Draw body
        canvas.drawRoundRect(-15, -10, 15, 15, 5, 5, bodyPaint);

        // Draw arms with swing
        canvas.save();
        canvas.rotate(-armSwing * 0.5f, -15, -5);
        canvas.drawRect(-18, -5, -13, 10, bodyPaint);
        canvas.restore();

        canvas.save();
        canvas.rotate(armSwing * 0.5f, 15, -5);
        canvas.drawRect(13, -5, 18, 10, bodyPaint);
        canvas.restore();

        // Draw head (bobbing)
        canvas.drawCircle(0, -20 - Math.abs(walkOffset * 0.5f), 12, headPaint);

        // Draw eyes
        canvas.drawCircle(-4, -22 - Math.abs(walkOffset * 0.5f), 2, eyePaint);
        canvas.drawCircle(4, -22 - Math.abs(walkOffset * 0.5f), 2, eyePaint);

        canvas.restore();
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}