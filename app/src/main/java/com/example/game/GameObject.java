package com.example.game;

import android.graphics.*;

public class GameObject {
    private float x, y;
    private float speedX, speedY;
    private int type;
    private float rotation = 0;
    private float rotationSpeed;
    private float scale = 1f;
    private float scaleDirection = 0.01f;
    private Paint paint;

    // Animation
    private float animationTime = 0;

    public GameObject(float x, float y, int type) {
        this.x = x;
        this.y = y;
        this.type = type;

        paint = new Paint();
        paint.setAntiAlias(true);

        // Set properties based on type
        switch (type) {
            case 0: // Floating star
                speedX = -3;
                speedY = 0;
                rotationSpeed = 2;
                paint.setColor(Color.YELLOW);
                break;
            case 1: // Bouncing circle
                speedX = -5;
                speedY = 2;
                paint.setColor(Color.CYAN);
                break;
            case 2: // Sine wave object
                speedX = -4;
                speedY = 0;
                paint.setColor(Color.MAGENTA);
                break;
        }
    }

    public void update() {
        animationTime += 0.1f;

        // Basic movement
        x += speedX;

        // Type-specific movement
        switch (type) {
            case 0: // Rotating star
                rotation += rotationSpeed;
                y += Math.sin(animationTime) * 2;
                break;

            case 1: // Bouncing
                y += speedY;
                if (y < 50 || y > 800) {
                    speedY = -speedY;
                }
                break;

            case 2: // Sine wave
                y += Math.sin(animationTime * 2) * 3;
                scale += scaleDirection;
                if (scale > 1.5f || scale < 0.5f) {
                    scaleDirection = -scaleDirection;
                }
                break;
        }
    }

    public void draw(Canvas canvas, Paint defaultPaint) {
        canvas.save();
        canvas.translate(x, y);
        canvas.rotate(rotation);
        canvas.scale(scale, scale);

        switch (type) {
            case 0: // Star
                drawStar(canvas, 0, 0, 30, 15, 5, paint);
                break;
            case 1: // Circle
                canvas.drawCircle(0, 0, 25, paint);
                break;
            case 2: // Diamond
                Path diamond = new Path();
                diamond.moveTo(0, -30);
                diamond.lineTo(20, 0);
                diamond.lineTo(0, 30);
                diamond.lineTo(-20, 0);
                diamond.close();
                canvas.drawPath(diamond, paint);
                break;
        }

        canvas.restore();
    }

    private void drawStar(Canvas canvas, float cx, float cy,
                          float outerRadius, float innerRadius, int numPoints, Paint paint) {
        Path path = new Path();
        double angle = Math.PI / numPoints;

        for (int i = 0; i < numPoints * 2; i++) {
            float radius = (i % 2 == 0) ? outerRadius : innerRadius;
            float x = cx + (float) (radius * Math.cos(i * angle - Math.PI / 2));
            float y = cy + (float) (radius * Math.sin(i * angle - Math.PI / 2));

            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        path.close();
        canvas.drawPath(path, paint);
    }

    public boolean isOffScreen(int screenWidth, int screenHeight) {
        return x < -100 || x > screenWidth + 100 ||
                y < -100 || y > screenHeight + 100;
    }
}