package com.example.game;

public class ArmorBuff {
    private boolean active = false;
    private long startTime;
    private static final long DURATION = 5000; // 5s
    public void activate() {
        active = true;
        startTime = System.currentTimeMillis();
    }
    public void update() {
        if (active && System.currentTimeMillis() - startTime > DURATION) active = false;
    }
    public boolean isActive() { return active; }
    public int reduceDamage(int dmg) {
        if (active) return dmg/2;
        return dmg;
    }
}