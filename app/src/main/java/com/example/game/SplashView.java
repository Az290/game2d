package com.example.game;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class SplashView extends SurfaceView implements SurfaceHolder.Callback {

    private Bitmap background;
    private Bitmap logo;
    private Paint buttonPaint, textPaint, copyrightPaint;
    private RectF buttonRect;
    private Context context;
    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }

    public SplashView(Context context) {
        super(context);
        this.context = context;
        getHolder().addCallback(this);

        background = BitmapFactory.decodeResource(getResources(), R.drawable.splash_bg); // bạn cần file này
        logo = BitmapFactory.decodeResource(getResources(), R.drawable.logo); // hoặc có thể bỏ

        buttonPaint = new Paint();
        buttonPaint.setColor(Color.parseColor("#3F51B5")); // màu nút

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(50f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        copyrightPaint = new Paint();
        copyrightPaint.setColor(Color.LTGRAY);
        copyrightPaint.setTextSize(30f);
        copyrightPaint.setTextAlign(Paint.Align.CENTER);

        buttonRect = new RectF(300, 1000, 780, 1120); // bạn có thể chỉnh lại theo màn hình thực tế
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        computeButtonRect(getWidth(), getHeight());
        drawScreen();
    }

    private void drawScreen() {
        Canvas canvas = getHolder().lockCanvas();

        if (canvas != null) {
            int width = getWidth();
            int height = getHeight();

            Rect dstRect = new Rect(0, 0, width, height);
            canvas.drawBitmap(background, null, dstRect, null);

            if (logo != null) {
                int logoX = (width - logo.getWidth()) / 2;
                canvas.drawBitmap(logo, logoX, 200, null);
            }

            canvas.drawRoundRect(buttonRect, 20, 20, buttonPaint);
            canvas.drawText("BẮT ĐẦU GAME", buttonRect.centerX(), buttonRect.centerY() + 15, textPaint);

            canvas.drawText("GAME ĐƯỢC XÂY DỰNG BỞI NHÓM I LỚP L03", width / 2f, height - 60, copyrightPaint);

            getHolder().unlockCanvasAndPost(canvas);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();

            if (buttonRect.contains(x, y)) {
                Intent intent = new Intent(context, FirstGameActivity.class);
                context.startActivity(intent);
                ((Activity) context).finish();
                return true;
            }
        }
        return super.onTouchEvent(event);
    }


    @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
    @Override public void surfaceDestroyed(SurfaceHolder holder) {}
    private void computeButtonRect(int width, int height) {
        float btnW = width * 0.5f;    // 50% chiều rộng màn hình
        float btnH = 120f;            // cao 120px (có thể đổi tùy cần)
        float left = (width - btnW) / 2f;
        float top = height * 0.65f;   // khoảng 65% chiều cao
        buttonRect = new RectF(left, top, left + btnW, top + btnH);
    }
}