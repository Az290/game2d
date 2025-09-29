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

    // Danh sách enemy trong wave hiện tại
    private List<Enemy> enemies = new ArrayList<>();

    // Delay giữa các wave
    private long lastWaveTime = 0;
    private long waveDelay = 2000; // ms

    public WaveManager(Context context, int screenWidth, int screenHeight) {
        this.context = context;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        spawnNextWave(); // Spawn wave đầu tiên
    }

    /** Gọi liên tục trong GameView.update() */
    public void update() {
        // Cập nhật enemy còn sống
        for (Enemy e : enemies) {
            e.update();
        }

        // Nếu tất cả enemy đã bị remove -> spawn wave mới
        if (enemies.isEmpty() && System.currentTimeMillis() - lastWaveTime > waveDelay) {
            spawnNextWave();
            lastWaveTime = System.currentTimeMillis();
        }
    }

    /** Sinh wave mới */
    private void spawnNextWave() {
        currentWave++;
        enemies.clear();

        if (currentWave % 5 == 0) {
            // Boss wave
            enemies.add(new Enemy(context, screenWidth, screenHeight, 99));
        } else {
            // Enemy thường, số lượng tăng dần theo wave
            int count = 3 + currentWave; // Wave 1 có ~4 enemy, tăng dần
            for (int i = 0; i < count; i++) {
                int type = random.nextInt(3); // enemy type: 0,1,2
                Enemy e = new Enemy(context, screenWidth, screenHeight, type);
                // rải vị trí spawn ngang màn hình
                float spawnX = (i+1) * (screenWidth/(count+1f));
                e.setPosition(spawnX, -100 - i*40);
                enemies.add(e);
            }
        }
    }

    /** Lấy danh sách enemy */
    public List<Enemy> getActiveEnemies() {
        return enemies;
    }

    /** Lấy wave hiện tại */
    public int getCurrentWave() {
        return currentWave;
    }
}