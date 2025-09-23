package com.example.game;

public class GamePlayer {
    // Thông số level
    private int level = 1;
    private int xp = 0;
    private int xpToNextLevel = 100;
    private static final int MAX_LEVEL = 5;

    private static final float BASE_DAMAGE = 25;
    private static final float DAMAGE_PER_LEVEL = 5;
    private static final float XP_SCALE_FACTOR = 1.5f;

    public GamePlayer() {}

    public boolean addXP(int amount) {
        if (level >= MAX_LEVEL) return false;
        xp += amount;
        if (xp >= xpToNextLevel) {
            levelUp();
            return true;
        }
        return false;
    }

    private void levelUp() {
        level++;
        xp -= xpToNextLevel;
        xpToNextLevel = (int)(xpToNextLevel * XP_SCALE_FACTOR);
        if (level > MAX_LEVEL) {
            level = MAX_LEVEL;
            xp = 0;
        }
    }

    public int getBulletCount() {
        if (level >= 5) return 3;
        else if (level >= 3) return 2;
        else return 1;
    }

    public int getCurrentDamage() {
        return (int)(BASE_DAMAGE + (level - 1) * DAMAGE_PER_LEVEL);
    }

    public int getLevel() { return level; }
    public int getXP() { return xp; }
    public int getXPToNextLevel() { return xpToNextLevel; }
    public float getXPProgress() { return (float)xp / xpToNextLevel; }
    public boolean isMaxLevel() { return level >= MAX_LEVEL; }
}