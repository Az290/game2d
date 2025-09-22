package com.example.game;

import android.graphics.*;
import java.util.ArrayList;
import java.util.Random;

public class ParticleEffect {
    private ArrayList<Particle> particles;
    private float x, y;
    private Random random;

    public ParticleEffect(float x, float y) {
        this.x = x;
        this.y = y;
        particles = new ArrayList<>();
        random = new Random();

        // Create initial particles
        for (int i = 0; i < 20; i++) {
            particles.add(new Particle(x, y));
        }
    }

    public void update() {
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.update();
            if (p.isDead()) {
                particles.remove(i);
            }
        }
    }

    public void draw(Canvas canvas, Paint paint) {
        for (Particle p : particles) {
            p.draw(canvas, paint);
        }
    }

    public boolean isFinished() {
        return particles.isEmpty();
    }

    private class Particle {
        float x, y;
        float vx, vy;
        float life;
        int color;
        float size;

        Particle(float x, float y) {
            this.x = x;
            this.y = y;
            this.vx = random.nextFloat() * 10 - 5;
            this.vy = random.nextFloat() * 10 - 5;
            this.life = 1f;
            this.size = random.nextFloat() * 10 + 5;
            this.color = Color.rgb(
                    random.nextInt(256),
                    random.nextInt(256),
                    random.nextInt(256)
            );
        }

        void update() {
            x += vx;
            y += vy;
            vy += 0.5f; // gravity
            life -= 0.02f;
            size *= 0.98f;
        }

        void draw(Canvas canvas, Paint paint) {
            paint.setColor(color);
            paint.setAlpha((int)(life * 255));
            canvas.drawCircle(x, y, size, paint);
        }

        boolean isDead() {
            return life <= 0 || size <= 0;
        }
    }
}