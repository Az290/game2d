package com.example.game;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WaveManager {
    private Context context;
    private int screenWidth, screenHeight;
    private int currentWave = 0;
    private Random random = new Random();

    // Danh sách enemy đang hoạt động
    private List<Enemy> enemies = new ArrayList<>();

    // Để kiểm tra tạo wave tiếp theo
    private long lastWaveTime = 0;
    private long waveDelay = 2000; // 2s delay giữa các wave

    public WaveManager(Context context, int screenWidth, int screenHeight) {
        this.context = context;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        spawnNextWave();
    }

    /** Được gọi mỗi frame từ GameView.update() */
    public void update() {
        boolean allDead = true;
        for (Enemy e : enemies) {
            if (!e.isDead()) {
                allDead = false;
                break;
            }
        }

        long now = System.currentTimeMillis();

        if (allDead && now - lastWaveTime > waveDelay) {
            spawnNextWave();
            lastWaveTime = now;
        }

        for (Enemy e : enemies) {
            e.update();
        }
    }

    private void spawnNextWave() {
        currentWave++;
        enemies.clear();

        if (currentWave % 5 == 0) {
            // Boss wave
            enemies.add(new Enemy(context, screenWidth, screenHeight, 99));
        } else {
            int count = 3 + currentWave;
            for (int i = 0; i < count; i++) {
                int type = random.nextInt(3); // 0,1,2
                Enemy e = new Enemy(context, screenWidth, screenHeight, type);
                float spawnX = (i+1) * (screenWidth/(count+1f));
                e.setPosition(spawnX, -100 - i*40);
                enemies.add(e);
            }
        }
    }

    public List<Enemy> getActiveEnemies() {
        return enemies;
    }

    public int getCurrentWave() {
        return currentWave;
    }
}