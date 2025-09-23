package com.example.game;

import android.graphics.*;
import java.util.ArrayList;
import java.util.Random;

public class LevelUpEffect {
    private ArrayList<LevelParticle> particles;
    private float x, y;
    private Random random;
    private boolean finished = false;
    private long startTime;
    private static final long DURATION = 1500; // Hiệu ứng kéo dài 1.5 giây

    public LevelUpEffect(float x, float y) {
        this.x = x;
        this.y = y;
        particles = new ArrayList<>();
        random = new Random();
        startTime = System.currentTimeMillis();

        // Tạo particles đặc biệt cho hiệu ứng lên cấp
        for (int i = 0; i < 30; i++) {
            particles.add(new LevelParticle(x, y));
        }
    }

    public void update() {
        // Kiểm tra thời gian tồn tại
        if (System.currentTimeMillis() - startTime > DURATION) {
            finished = true;
            return;
        }

        for (LevelParticle p : particles) {
            p.update();
        }
    }

    public void draw(Canvas canvas, Paint paint) {
        if (finished) return;

        for (LevelParticle p : particles) {
            p.draw(canvas, paint);
        }
    }

    public boolean isFinished() {
        return finished;
    }

    private class LevelParticle {
        float x, y;
        float vx, vy;
        float life;
        int color;
        float size;
        float angle = 0;
        float rotationSpeed;

        LevelParticle(float x, float y) {
            this.x = x;
            this.y = y;

            // Vận tốc ngẫu nhiên theo hướng
            double angle = random.nextDouble() * Math.PI * 2;
            float speed = random.nextFloat() * 5 + 2;
            this.vx = (float) (Math.cos(angle) * speed);
            this.vy = (float) (Math.sin(angle) * speed);

            this.life = 1f;
            this.size = random.nextFloat() * 15 + 5;
            this.rotationSpeed = random.nextFloat() * 10 - 5;

            // Màu sắc đặc biệt cho hiệu ứng lên cấp (vàng, cam, trắng)
            int[] colors = {
                    Color.rgb(255, 215, 0),  // Vàng
                    Color.rgb(255, 255, 255), // Trắng
                    Color.rgb(255, 165, 0)   // Cam
            };
            this.color = colors[random.nextInt(colors.length)];
        }

        void update() {
            x += vx;
            y += vy;

            // Giảm vận tốc dần
            vx *= 0.98f;
            vy *= 0.98f;

            // Giảm kích thước và độ trong suốt dần
            life -= 0.01f;
            size *= 0.99f;

            // Xoay particle
            angle += rotationSpeed;
        }

        void draw(Canvas canvas, Paint paint) {
            paint.setColor(color);
            paint.setAlpha((int)(life * 255));

            canvas.save();
            canvas.translate(x, y);
            canvas.rotate(angle);

            // Vẽ hình ngôi sao
            drawStar(canvas, 0, 0, size, size/2, 5, paint);

            canvas.restore();
        }

        private void drawStar(Canvas canvas, float cx, float cy,
                              float outerRadius, float innerRadius,
                              int numPoints, Paint paint) {
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
    }
}