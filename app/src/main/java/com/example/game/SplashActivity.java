package com.example.game;

import android.app.Activity;
import android.os.Bundle;

public class SplashActivity extends Activity {

    SplashView splashView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        splashView = new SplashView(this);
        setContentView(splashView);
    }
}