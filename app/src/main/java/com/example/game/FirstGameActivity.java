package com.example.game;

import android.app.Activity;
import android.os.Bundle;

public class FirstGameActivity extends Activity {

    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gameView = new GameView(this);
        setContentView(gameView);
    }
    @Override protected void onDestroy() {
        super.onDestroy();
        if (gameView != null) gameView.release();
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.resume();   // ✅ đây mới gọi bgMusic.start()
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameView.pause();    // ✅ pause nhạc khi app minimize
    }
}
